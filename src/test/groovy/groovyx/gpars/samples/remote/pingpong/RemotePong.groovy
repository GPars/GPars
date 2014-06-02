package groovyx.gpars.samples.remote.pingpong

import groovyx.gpars.actor.Actors

// --- conecpt ---

def pongActor = Actors.actor {
    println "Pong Actor"

    // register myself as remote actor
    // RemoteActors.register(this, ...)

    // handle incomming messages
    // loop -> react
}

pongActor.join()