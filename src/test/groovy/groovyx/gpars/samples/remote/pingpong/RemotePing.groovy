package groovyx.gpars.samples.remote.pingpong

import groovyx.gpars.actor.Actors

// --- conecpt ---

def pingActor = Actors.actor {
    println "Ping Actor"

    react { numberOfPings ->
        println numberOfPings

        // get remote Pong Actor
        // RemoteActors.get(host, port, class?)

        loop(numberOfPings) {
            println "PING"
            // remotePongActor << "PING"
            // react on reply
        }
    }
}

pingActor << 7
pingActor.join()