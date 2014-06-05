package groovyx.gpars.samples.remote.calculator

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.ClientNettyTransportProvider

import java.util.concurrent.CountDownLatch;

def queryActor = Actors.actor {
    println "Remote Calculator - Query"

    def remoteCalculator = RemoteActors.get("localhost", 9000)

    remoteCalculator << 1
    remoteCalculator << 2

    react { println it }

    remoteCalculator.stop() // client never closes?
}

queryActor.join()

RemoteActors.shutdown()