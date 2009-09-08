//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.samples.actors

import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.PooledActorGroup

/**
 * A popular gae implemented with actors.
 * Notice the use of a PooledActorGroup to hold the actors participating in the game.
 */

enum Move {
    ROCK, PAPER, SCISSORS
}

random = new Random()

def randomMove() {
    return Move.values()[random.nextInt(Move.values().length)]
}

def announce(p1, m1, p2, m2) {
    String winner = "tie"
    switch ([m1, m2]) {
        case [[Move.ROCK, Move.SCISSORS], [Move.PAPER, Move.ROCK], [Move.SCISSORS, Move.PAPER]]:
            winner = p1
            break
        default:
            if (m1 != m2) winner = p2
    }

    [[p1, m1], [p2, m2]].sort {it[0]}.each { print "${it[0]}\t(${it[1]}),\t\t" }
    println "winner = ${winner}"
}

PooledActorGroup group = new PooledActorGroup()
group.with {
    final def player1 = actor {
        loop {
            react {
                reply(["Player 1", randomMove()])
            }
        }
    }.start()

    final def player2 = actor {
        loop {
            react {
                reply(["Player 2", randomMove()])
            }
        }
    }.start()

    def coordinator = actor {
        loop {
            react {
                player1.send("play")
                player2.send("play")

                react {msg1 ->
                    react {msg2 ->
                        announce(msg1[0], msg1[1], msg2[0], msg2[1])
                        send("start")
                    }
                }
            }
        }
    }.start()

    coordinator.send("start")
}

System.in.read()
group.shutdown()
