// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.operator.DataflowOperator
import groovyx.gpars.group.NonDaemonPGroup

/**
 * A concurrent implementation of the Game of Life using dataflow operators
 * Inspired by https://github.com/mcmenaminadrian/Groovy-Life/blob/master/Life.groovy
 *
 * Each cell of the world is represented by a DataflowBroadcast instance, which emits the current value to all subscribed listeners.
 * The printGrid() method is one of these listeners, so it can show the current state of the world to the user.
 * To transform an old world into a new one, a dataflow operator exists for each cell, monitoring the cell as well as the surroundings of the cell
 * and calculating the new value for the cell, whenever all the monitored cells emit new values. The calculated value is written
 * back into the cell and so it can be observed by all interested operators in the next iteration of the system.
 *
 * The system iterates spontaneously without any external clock or synchronization. The inherent quality of operators to wait for all input values
 * before proceeding guarantees that the system evolves in phases/generations.
 *
 * @author Vaclav Pech
 */

class LifeGameWithDataflowOperators {
/* Controls the game */
    final def initialGrid = []  //initial values entered by the user
    final List<List<DataflowBroadcast>> channelGrid = []  //the sequence of life values (0 or 1) for each cell
    final List<List<DataflowReadChannel>> printingGrid = []
    //the sequence of life values (0 or 1) for each cell to read by the print method
    final List<List<DataflowOperator>> operatorGrid = []
    //the grid of operators calculating values for their respective cells
    final DataflowBroadcast heartbeats = new DataflowBroadcast()  //gives pace to the calculation
    def gridWidth
    def gridHeight
    final def group = new NonDaemonPGroup()  //the thread pool to use by all the operators

    void run() {
        welcome()
        printInitialGrid()
        setupIndividuals()
        evolve(0)

    }

    void welcome() {
        println "Welcome to the Game of Life"
        println "Based on John Conway's famous article for the Scientific American."
        println "To begin you must specify a grid size. Height and width are limited"
        println "to 40 x 40 max."
        println()
        println "Please specify the width and height of your grid in the format"
        println "width,height"
        getGridDimensions()

        setupCells()
        setupOperators()

    }

    private def setupOperators() {
        (0..<gridHeight).each { rowIndex ->
            def operatorRow = []
            (0..<gridWidth).each { columnIndex ->
                def inputChannels = [channelGrid[rowIndex][columnIndex].createReadChannel()]
                [rowIndex - 1, rowIndex, rowIndex + 1].each { currentRowIndex ->
                    if (currentRowIndex in 0..<gridHeight) {
                        if (columnIndex > 0) inputChannels.add(channelGrid[currentRowIndex][columnIndex - 1].createReadChannel())
                        if (currentRowIndex != rowIndex) inputChannels.add(channelGrid[currentRowIndex][columnIndex].createReadChannel())
                        if (columnIndex < gridHeight - 1) inputChannels.add(channelGrid[currentRowIndex][columnIndex + 1].createReadChannel())
                    }
                }
                inputChannels.add(heartbeats.createReadChannel())

                final Closure code = new LifeClosure(this, inputChannels.size)
                operatorRow[columnIndex] = group.operator(inputs: inputChannels, outputs: [channelGrid[rowIndex][columnIndex]], code)
            }
            operatorGrid.add(operatorRow)
        }
    }

    private def setupCells() {
        (0..<gridHeight).each {
            def initialRow = []
            def valueRow = []
            List<DataflowBroadcast> channelRow = []
            (0..<gridWidth).each {
                initialRow[it] = 0
                channelRow[it] = new DataflowBroadcast()
                valueRow[it] = channelRow[it].createReadChannel()
            }
            initialGrid.add(initialRow)
            printingGrid.add(valueRow)
            channelGrid.add(channelRow)
        }
    }

    void setupIndividuals() {
        println()
        println "You now need to specify the cells in which the bacteria live."
        println "Please enter in the format (x, y). Enter -1, -1 to end this phase."
        getCellPlaces()
        println "Game will now begin..."
    }

    void evolve(def generation) {
        //initialize the dataflow network by copying the values to the cells (channels)
        (0..<gridHeight).each { rowIndex ->
            (0..<gridWidth).each { columnIndex ->
                channelGrid[rowIndex][columnIndex] << initialGrid[rowIndex][columnIndex]
            }
        }

        while (true) {
            heartbeats << 'go!'
            //This message is sent to all operators to trigger the calculation of the next generation
            println "Generation $generation"
            printGrid()
            ++generation
            sleep 500
        }
    }

    void getCellPlaces() {
        String gridPoint = new Scanner(System.in).nextLine()
        def comma = ","
        def gridBits = gridPoint.tokenize(comma)
        if (gridBits.isEmpty()) {
            errorXY(gridPoint + " does not evaluate")
            getCellPlaces()
            return
        }
        if (gridBits[1] == null) {
            errorXY(gridPoint + " badly formed")
            getCellPlaces()
            return
        }
        if (!gridBits[0].isInteger() || !gridBits[1].isInteger()) {
            errorXY(gridPoint + " not numerical")
            getCellPlaces()
            return
        }
        def gridW = gridBits[0].toInteger()
        def gridH = gridBits[1].toInteger()
        if ((gridW == -1) && (gridH == -1))
            return
        if (!(gridW in 1..gridWidth) || !(gridH in 1..gridHeight)) {
            errorXY(gridPoint + " out of range")
            getCellPlaces()
            return
        }
        initialGrid[gridH - 1][gridW - 1] = 1;
        printInitialGrid()
        println "Next place x,y... or -1,-1 to finish"
        getCellPlaces()
        return
    }

    void getGridDimensions() {
        String gridSize = new Scanner(System.in).nextLine()
        def comma = ","
        def gridBits = gridSize.tokenize(comma)
        if (gridBits.isEmpty()) {
            errorXY(gridSize + " does not evaluate")
            getGridDimensions()
            return
        }
        if (gridBits[1] == null) {
            errorXY(gridSize + " badly formed")
            getGridDimensions()
            return
        }
        if (!gridBits[0].isInteger() || !gridBits[1].isInteger()) {
            errorXY(gridSize + " not numerical")
            getGridDimensions()
            return
        }
        gridWidth = gridBits[0].toInteger()
        gridHeight = gridBits[1].toInteger()
        if (!(gridWidth in 1..40) || !(gridHeight in 1..40)) {
            errorXY(gridSize + "out of range")
            getGridDimensions()
        }
    }

    void errorXY(String strMsg) {
        println strMsg
    }

    void printInitialGrid() {
        (0..gridWidth + 1).each {
            print "*"
        }
        println()
        (0..<gridHeight).each {
            def line = it
            print "|"
            (0..<gridWidth).each {
                if (initialGrid[line][it] == 0)
                    print " "
                else
                    print "X"
            }
            print "|"
            println()
        }
        (0..gridWidth + 1).each {
            print "*"
        }
        println()
    }

    void printGrid() {
        (0..gridWidth + 1).each {
            print "*"
        }
        println()
        (0..<gridHeight).each {
            def line = it
            print "|"
            (0..<gridWidth).each {
                if (printingGrid[line][it].val == 0)
                    print " "
                else
                    print "X"
            }
            print "|"
            println()
        }
        (0..gridWidth + 1).each {
            print "*"
        }
        println()
    }
}

def lifer = new LifeGameWithDataflowOperators()
lifer.run()

class LifeClosure extends Closure {
    final int numberOfArguments

    LifeClosure(final Object owner, final int numberOfArguments) {
        super(owner)
        this.numberOfArguments = numberOfArguments
    }

    @Override
    int getMaximumNumberOfParameters() {
        return numberOfArguments
    }

    @Override
    Object call(Object[] args) {
        def result = args[0]
        def mates = args[1..-2].findAll { it > 0 }.size()
        if (mates > 3) result = 0
        else if (mates == 3) result = 1
        else if (result == 1 && mates == 2)
            result = 1
        else
            result = 0

        bindOutput result
    }
}