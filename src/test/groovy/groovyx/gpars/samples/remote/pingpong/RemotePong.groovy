package groovyx.gpars.samples.remote.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.netty.NettyTransportProvider

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

NettyTransportProvider.startServer("localhost", 9000)
// register pongActor as a remote actor
RemoteActors.register(pongActor, "pong")

pongActor.join()