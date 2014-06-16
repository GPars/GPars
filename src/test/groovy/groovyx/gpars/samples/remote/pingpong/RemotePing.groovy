package groovyx.gpars.samples.remote.pingpong

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

import java.util.concurrent.CountDownLatch

def pingActor = Actors.actor {
    println "Ping Actor"

    // get remote pongActor
    def remotePongActorFuture = RemoteActors.get("localhost", 9000, "pong")
    def remotePongActor = remotePongActorFuture.get()

    def thankYou = {
        remotePongActor << "STOP"
    }

    react { numberOfPings ->
        loop(numberOfPings, thankYou) {
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