// GPars - Groovy Parallel Systems
//
// Copyright © 2008-12  The original author or authors
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

package groovyx.gpars.samples.dataflow.kanban

import groovyx.gpars.dataflow.KanbanFlow

import groovyx.gpars.dataflow.KanbanTray

import static groovyx.gpars.dataflow.ProcessingNode.node

/**
 * A simple version of a concurrent Game of Live using KanbanFlow where the printing of the
 * current state happens concurrently to calculating the next iteration.
 * Noteworthy: processing units are stateless, coordination is implicit.
 * The demo shows a classic "glider" moving from the upper left to the lower right corner.
 * @author Dierk Koenig
 */

List<List<Integer>> makeBoard() { (0..9).collect { [0] * 10 } }

def display = node { boardOut, boardIn  ->
    def board = boardIn.take()
    boardOut << board                   // downstream can proceed concurrently
    board.each { println it.join(' ') }
    println()
}

def nextBoard = node { boardIn, boardOut ->
    def board = boardIn.take()
    def out   = makeBoard()
    for (row in 1..8) { // we could use eachParallel here but that is a different story
        for (col in 1..8) {
            out[row][col] = nextCellValue(board, row, col)
        }
    }
    boardOut << out
}

int nextCellValue(board, row, col) {
    def aliveNeighbors =
        board[row - 1][col - 1] + board[row - 1][col] + board[row - 1][col + 1] +
        board[row + 0][col - 1] +                       board[row + 0][col + 1] +
        board[row + 1][col - 1] + board[row + 1][col] + board[row + 1][col + 1]
    switch(aliveNeighbors){
        case 0..1 : return 0
        case 2    : return board[row][col]
        case 3    : return 1
        case 4..8 : return 0
    }
}

new KanbanFlow().with {
    cycleAllowed = true
    def down = link display   to nextBoard
    def loop = link nextBoard to display

    def startBoard = makeBoard()
    [[1,2],[2,3],[3,1],[3,2],[3,3]].each { startBoard[it[0]][it[1]] = 1 } // glider
    loop.downstream << new KanbanTray(link: loop, product: startBoard)

    start()
    sleep 100
    stop()
}