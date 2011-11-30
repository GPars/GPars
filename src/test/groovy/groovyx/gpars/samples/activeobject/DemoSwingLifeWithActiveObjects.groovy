// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.samples.activeobject

import groovy.swing.SwingBuilder
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveObjectRegistry
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.NonDaemonPGroup
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.metal.MetalLookAndFeel

/**
 * A concurrent implementation of the Game of Life using active objects
 *
 * Each cell of the world is represented by an active object, which, upon receiving a heartbeat, emits its current state to its neighbors.
 * It also listens to states reported by the neighbors and uses that information to update its state.
 * A heart-beating mechanism ensures the whole population evolves in discrete steps.
 *
 * @author Vaclav Pech
 */

ActiveObjectRegistry.instance.register("GOLDemo", new NonDaemonPGroup(8))

new SwingLifeGameWithActiveObjects(30, 20).run()

@ActiveObject("GOLDemo")
final class Cell {
    private boolean alive
    private final int row
    private final int col
    private int numAliveNeighbors
    private int numEmptyNeighbors
    final List<Cell> neighbors = []
    private SwingLifePrinter printer
    private SwingLifeGameWithActiveObjects owner

    Cell(final int row, final int col, final boolean alive, SwingLifePrinter printer, SwingLifeGameWithActiveObjects owner) {
        this.alive = alive
        this.row = row
        this.col = col
        this.printer = printer
        this.owner = owner
    }

    @ActiveMethod
    void heartBeat() {
        numEmptyNeighbors += 1
        neighbors.each {alive ? it.reportBeingAlive() : it.reportBeingEmpty()}
        progress()
    }

    @ActiveMethod
    void reportBeingAlive() {
        numAliveNeighbors += 1
        progress()
    }

    @ActiveMethod
    void reportBeingEmpty() {
        numEmptyNeighbors += 1
        progress()
    }

    private void progress() {
        if (numAliveNeighbors + numEmptyNeighbors == neighbors.size() + 1) {
            if (numAliveNeighbors > 3) alive = false
            else if (numAliveNeighbors == 3) alive = true
            else if (alive && numAliveNeighbors == 2)
                alive = true
            else
                alive = false
            initializeCounters()
            printer.printMe(row, col, alive)
            owner.done()
        }
    }

    private void initializeCounters() {
        this.numAliveNeighbors = 0
        this.numEmptyNeighbors = 0
    }
}

@ActiveObject("GOLDemo")
final class SwingLifeGameWithActiveObjects {
    /* Controls the game */
    private final List<List<Cell>> cellGrid = []
    private SwingLifePrinter printer

    private final gridWidth
    private final gridHeight
    //the thread pool to use by all the active objects

    private final SwingBuilder builder = new SwingBuilder()
    private JFrame frame
    private JLabel iteration
    private JPanel scene

    private int finishedCells
    private final int totalCells
    private int generation = 0
    boolean running = false

    SwingLifeGameWithActiveObjects(final gridWidth, final gridHeight) {
        ActiveObjectRegistry.instance.register("GOLDemoGUI", new DefaultPGroup(1))
        this.gridWidth = gridWidth
        this.gridHeight = gridHeight
        setupUI()
        setupCells()
        totalCells = gridWidth * gridHeight
    }

    @ActiveMethod
    void run() {
        evolve()
    }

    private void setupUI() {
        final List<List<JButton>> visualCells = []  //refers to the visual cells in the UI

        UIManager.setLookAndFeel(new MetalLookAndFeel())
        frame = builder.frame(title: "Game of Life", defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
            vbox {
                hbox {
                    JLabel caption = label('Iteration # ')
                    iteration = label('0')
                    final Font font = new Font(caption.font.name, caption.font.style, 18)
                    caption.font = font
                    iteration.font = font
                }

                scene = builder.panel()
                scene.layout = new GridLayout(gridHeight, gridWidth)
                (0..<gridHeight).each {rowIndex ->
                    def cellRow = []
                    (0..<gridWidth).each {columnIndex ->
                        final JPanel cell = builder.panel()
                        scene.add(cell)
                        def b = builder.button(' ', enabled: false)
                        cell.add(b)
                        cellRow.add(b)
                    }
                    visualCells.add(cellRow)
                }
                hbox {
                    button(text: 'Start', id: 'startButton', actionPerformed: {
                        startEvolution()
                        startButton.enabled = false
                        pauseButton.enabled = true

                    })
                    button(text: 'Pause', id: 'pauseButton', enabled: false, actionPerformed: {
                        pauseEvolution()
                        pauseButton.enabled = false
                        startButton.enabled = true
                    })
                }
            }
        }
        frame.visible = true
        frame.pack()
        printer = new SwingLifePrinter(visualCells)
    }

    private def setupCells() {
        final Random random = new Random()
        (0..<gridHeight).each {rowIndex ->
            def cellRow = []
            (0..<gridWidth).each {colIndex ->
                cellRow[colIndex] = new Cell(rowIndex, colIndex, randomInitialValue(random), printer, this)
            }
            cellGrid.add(cellRow)
        }

        (0..<gridHeight).each {rowIndex ->
            (0..<gridWidth).each {columnIndex ->
                final List<Cell> neighbors = []
                [rowIndex - 1, rowIndex, rowIndex + 1].each {currentRowIndex ->
                    if (currentRowIndex in 0..<gridHeight) {
                        if (columnIndex > 0) neighbors << cellGrid[currentRowIndex][columnIndex - 1]
                        if (currentRowIndex != rowIndex) neighbors << cellGrid[currentRowIndex][columnIndex]
                        if (columnIndex < gridWidth - 1) neighbors << cellGrid[currentRowIndex][columnIndex + 1]
                    }
                }
                cellGrid[rowIndex][columnIndex].neighbors.addAll(neighbors)
            }
        }

    }

    private boolean randomInitialValue(final Random random) {
        final int value = random.nextInt(100)
        return value > 49 ? true : false
    }

    @ActiveMethod
    void startEvolution() {
        if (!running) {
            running = true
            evolve()
        }
    }

    @ActiveMethod
    void pauseEvolution() {
        running = false
    }

    @ActiveMethod
    void done() {
        finishedCells += 1
        if (finishedCells == totalCells) {
            finishedCells = 0
            if (running) evolve()
        }
    }

    private void evolve() {
        sleep 1000
        //Send heartbeats to all cells
        (0..<gridHeight).each {rowIndex ->
            (0..<gridWidth).each {columnIndex ->
                cellGrid[rowIndex][columnIndex].heartBeat()
            }
        }

        builder.edt {
            ++generation
            iteration.text = generation
        }
    }
}

@ActiveObject("GOLDemoGUI")
final class SwingLifePrinter {
    private final List<List<JButton>> visualCells

    SwingLifePrinter(final List<List<JButton>> visualCells) {
        this.visualCells = visualCells
    }

    @ActiveMethod
    void printMe(final int row, final int col, final boolean aliveFlag) {
        final cell = visualCells[row][col]
        final localAliveFlag = aliveFlag
        SwingUtilities.invokeLater {
            cell.background = localAliveFlag ? Color.BLUE : Color.WHITE
        }
    }
}