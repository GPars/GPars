package groovyx.gpars.samples.remote.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

def pongActor = Actors.actor {
    println "Pong Actor"

    // handle incomming messages
    loop({true}) {
        react {
            println it
            // reply "PONG"
        }
    }
}

// register pongActor as a remote actor
RemoteActors.register(pongActor)

pongActor.join()