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

package groovyx.gpars.samples.actors

import groovy.swing.SwingBuilder
import groovy.transform.Immutable
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.actor.StaticDispatchActor
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.samples.activeobject.Cell
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import java.util.concurrent.Semaphore
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

new SwingLifeGameWithActors(30, 20).run()

final class CellActor extends DynamicDispatchActor {
    private boolean alive
    private final int row
    private final int col
    private int numAliveNeighbors
    private int numEmptyNeighbors
    final List<Cell> neighbors = []
    private PrinterActor printer
    private SwingLifeGameWithActors owner

    CellActor(final int row, final int col, final boolean alive, PrinterActor printer, SwingLifeGameWithActors owner) {
        this.alive = alive
        this.row = row
        this.col = col
        this.printer = printer
        this.owner = owner
    }

    void onMessage(Heartbeat heartbeat) {
        numEmptyNeighbors += 1
        neighbors.each {it.send(alive)}
        progress()
    }

    void onMessage(Boolean alive) {
        if (alive) numAliveNeighbors += 1
        else numEmptyNeighbors += 1
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
            printer.send(new PrintMessage(row, col, alive))
            owner.send('done')
        }
    }

    private void initializeCounters() {
        this.numAliveNeighbors = 0
        this.numEmptyNeighbors = 0
    }
}

final class SwingLifeGameWithActors extends StaticDispatchActor {
    /* Controls the game */
    private final List<List<CellActor>> cellGrid = []
    private PrinterActor printer

    private final gridWidth
    private final gridHeight
    //the thread pool to use by all the active objects

    private final SwingBuilder builder = new SwingBuilder()
    private JFrame frame
    private JLabel iteration
    private JPanel scene

    private final group = new NonDaemonPGroup(8)

    private final Semaphore nextGenerationPermit = new Semaphore(0)
    private int finishedCells
    private final int totalCells

    SwingLifeGameWithActors(final gridWidth, final gridHeight) {
        this.gridWidth = gridWidth
        this.gridHeight = gridHeight
        setupUI()
        setupCells()
        totalCells = gridWidth * gridHeight
    }

    void run() {
        this.start()
        evolve(0)
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
                        nextGenerationPermit.release()
                        startButton.enabled = false
                        pauseButton.enabled = true

                    })
                    button(text: 'Pause', id: 'pauseButton', enabled: false, actionPerformed: {
                        nextGenerationPermit.acquire()
                        pauseButton.enabled = false
                        startButton.enabled = true
                    })
                }
            }
        }
        frame.visible = true
        frame.pack()
        printer = new PrinterActor(visualCells).start()
    }

    private def setupCells() {
        final Random random = new Random()
        (0..<gridHeight).each {rowIndex ->
            def cellRow = []
            (0..<gridWidth).each {colIndex ->
                final CellActor actor = new CellActor(rowIndex, colIndex, randomInitialValue(random), printer, this)
                actor.parallelGroup = group
                actor.start()
                cellRow[colIndex] = actor
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

    @Override
    void onMessage(Object message) {
        finishedCells += 1
        if (finishedCells == totalCells) {
            finishedCells = 0
            nextGenerationPermit.release()
        }
    }

    private void evolve(def generation) {
        while (true) {
            //Send heartbeats to all cells
            (0..<gridHeight).each {rowIndex ->
                (0..<gridWidth).each {columnIndex ->
                    cellGrid[rowIndex][columnIndex].send(new Heartbeat())
                }
            }

            builder.edt {
                ++generation
                iteration.text = generation
            }
            nextGenerationPermit.acquire(2)
            sleep 1000
            nextGenerationPermit.release()
        }
    }
}

final class PrinterActor extends StaticDispatchActor {
    private final List<List<JButton>> visualCells

    PrinterActor(final List<List<JButton>> visualCells) {
        this.visualCells = visualCells
    }

    @Override
    void onMessage(Object message) {
        PrintMessage msg = (PrintMessage) message
        final cell = visualCells[msg.row][msg.col]
        SwingUtilities.invokeLater {
            cell.background = msg.aliveFlag ? Color.BLUE : Color.WHITE
        }
    }
}

@Immutable
final class Heartbeat {}

@Immutable
final class PrintMessage {
    final int row
    final int col
    final boolean aliveFlag
}