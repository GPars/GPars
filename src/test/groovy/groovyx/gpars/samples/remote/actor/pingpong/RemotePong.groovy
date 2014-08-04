package groovyx.gpars.samples.remote.actor.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9000

def pongActor = Actors.actor {
    println "Pong Actor"

    // handle incoming messages
    loop {
        react {
            println it
            reply "PONG"
        }
    }
}

def remoteActors = RemoteActors.create()
remoteActors.startServer HOST, PORT

// publish pongActor as a remote actor
remoteActors.publish pongActor, "pong"

pongActor.join()