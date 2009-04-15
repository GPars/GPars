package org.gparallelizer.samples

import org.gparallelizer.actors.pooledActors.PooledActors

enum Move {
    ROCK, PAPER, SCISSORS
}

random = new Random()

def randomMove() {
    return Move.values()[random.nextInt(Move.values().length)]
}

final def player1 = PooledActors.actor {
    loop {
        react {
            reply(["Player 1", randomMove()])
        }
    }
}.start()

final def player2 = PooledActors.actor {
    loop {
        react {
            reply(["Player 2", randomMove()])
        }
    }
}.start()

def coordinator = PooledActors.actor {
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

def announce(p1, m1, p2, m2) {
    String winner = "tie"
    switch ([m1, m2]) {
        case [[Move.ROCK, Move.SCISSORS], [Move.PAPER, Move.ROCK], [Move.SCISSORS, Move.PAPER]]:
            winner = p1
            break
        default:
            if (m1 != m2) winner = p2
    }

    [[p1, m1], [p2, m2]].sort{it[0]}.each { print "${it[0]}\t(${it[1]}),\t\t" }
    println "winner = ${winner}"
}

coordinator.send("start")