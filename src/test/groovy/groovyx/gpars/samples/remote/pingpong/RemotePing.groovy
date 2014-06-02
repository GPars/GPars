package groovyx.gpars.samples.remote.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

def pingActor = Actors.actor {
    println "Ping Actor"

    react { numberOfPings ->
        println numberOfPings

        // get remote pongActor
        RemoteActors.get("localhost", 9000) // class? name?

        loop(numberOfPings) {
            println "PING"
            // remotePongActor << "PING"
            // react on reply
        }
    }
}

pingActor << 7
pingActor.join()