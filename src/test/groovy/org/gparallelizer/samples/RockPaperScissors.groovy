package org.gparallelizer.samples

import org.gparallelizer.actors.pooledActors.PooledActors

enum Move { ROCK, PAPER, SCISSORS }

random = new Random()
def randomMove() {
  return Move.values()[random.nextInt(Move.values().length)]
}

final def player1 = PooledActors.actor {
  loop {
    react {
      reply (["Player 1", randomMove()])
    }
  }
}.start()

final def player2 = PooledActors.actor {
  loop {
    react {
      reply (["Player 2", randomMove()])
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
  if(firstWins(m1, m2) && ! firstWins(m2, m1)) {
    winner = p1
  } else if(firstWins(m2, m1) && ! firstWins(m1, m2)) {
    winner = p2
  } // else tie

  if(p1.compareTo(p2) < 0) {
    println toString(p1, m1) + ", " + toString(p2, m2) + ", winner = " + winner
  } else {
    println toString(p2, m2) + ", " + toString(p1, m1) + ", winner = " + winner
  }
}

def toString(player, move) {
  return player + " (" + move + ")"
}

def firstWins(Move m1, Move m2) {
  return (m1 == Move.ROCK && m2 == Move.SCISSORS) ||
    (m1 == Move.PAPER && m2 == Move.ROCK) ||
    (m1 == Move.SCISSORS && m2 == Move.PAPER)
}

coordinator.send("start")