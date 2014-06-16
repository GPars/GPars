package groovyx.gpars.samples.remote.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

def pongActor = Actors.actor {
    println "Pong Actor"

    def stop = false

    // handle incoming messages
    loop({ !stop }) {
        react {
            println it
            if (it.equals("STOP")) {
                stop = true;
            } else {
                reply "PONG"
            }
        }
    }
}

// register pongActor as a remote actor
RemoteActors.register(pongActor, "pong")

pongActor.join()