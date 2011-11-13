// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.samples.actors.stateful

import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroup
import groovy.transform.Immutable

/**
 * A popular game implemented with actors.
 * Notice the use of a DefaultPGroup to hold the actors participating in the game.
 * Author: Vaclav Pech, Dierk Koenig
 */

enum Move {
    ROCK, PAPER, SCISSORS
}

final /*static*/ BEATS = [
        [Move.ROCK, Move.SCISSORS],
        [Move.PAPER, Move.ROCK],
        [Move.SCISSORS, Move.PAPER]
].asImmutable()

@Immutable final class Stroke {
    String player;
    Move move
}

random = new Random()

def randomMove() {
    sleep random.nextInt(10) // mimic some longer interministic processing time
    return Move.values()[random.nextInt(Move.values().length)]
}

def announce = {Stroke first, Stroke second ->
    String winner = "tie"
    if ([first, second]*.move in BEATS) winner = first.player
    if ([second, first]*.move in BEATS) winner = second.player

    def out = new StringBuilder()
    [first, second].each { out << "${it.player} ${it.move.toString().padRight(8)}, " }
    out << "winner = ${winner}"
}

PGroup pooled = new NonDaemonPGroup() // uses default pool size

final player1 = pooled.reactor { new Stroke("Player 1", randomMove()) }
final player2 = pooled.reactor { new Stroke("Player 2", randomMove()) }

def coordinator = pooled.actor {
    int count = 0
    loop {
        count++
        if (count >= 120) {
            [player1, player2, delegate]*.terminate()
            terminate()
            return
        }
        react {
            player1.send()
            player2.send()
            react {Stroke first ->
                react {Stroke second ->
                    println announce(first, second)
                    send()
                }
            }
        }
    }
}

coordinator.send()
coordinator.join()
pooled.shutdown()