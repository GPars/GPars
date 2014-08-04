package groovyx.gpars.samples.remote.actor.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9000

def remoteActors = RemoteActors.create()

def pingActor = Actors.actor {
    println "Ping Actor"

    // get remote pongActor
    def remotePongActor = remoteActors.get HOST, PORT, "pong" get()

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

pingActor << 3
pingActor.join()