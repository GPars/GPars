package groovyx.gpars.samples.remote.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

def pingActor = Actors.actor {
    println "Ping Actor"

    // get remote pongActor
    def remotePongActor = RemoteActors.get("localhost", 9000) // class? name?

    react { numberOfPings ->
        loop(numberOfPings) {
            println "PING"
            remotePongActor << "PING"
            react {
                println it
            }
        }
    }
}

pingActor << 7
pingActor.join()