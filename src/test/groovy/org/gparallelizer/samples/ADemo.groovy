import static org.gparallelizer.actors.pooledActors.PooledActors.*

enum Move { ROCK, PAPER, SCISSORS }

random = new Random()
def randomMove() {
  return Move.values()[random.nextInt(Move.values().length)]
}

final def player1 = actor {
  loop {
    react {
      reply ["Player 1", randomMove()]
    }
  }
}.start()

final def player2 = actor {
  loop {
    react {
      reply ["Player 2", randomMove()]
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
          announce(msg1[1], msg1[2], msg2[1], msg2[2])
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
  return (m1 == ROCK && m2 == SCISSORS) ||
    (m1 == PAPER && m2 == ROCK) ||
    (m1 == SCISSORS && m2 == PAPER)
}

coordinator.send("start")