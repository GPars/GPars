package groovyx.gpars.samples.remote.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.netty.NettyTransportProvider

import java.util.concurrent.CountDownLatch

def pingActor = Actors.actor {
    println "Ping Actor"

    // get remote pongActor
    def remotePongActor = RemoteActors.get("localhost", 9000, "pong").get()

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

NettyTransportProvider.stopClients()