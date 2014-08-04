package groovyx.gpars.samples.remote.actor.calculator

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.netty.NettyTransportProvider

def queryActor = Actors.actor {
    println "Remote Calculator - Query"

    def remoteCalculator = RemoteActors.get("localhost", 9000, "remote-calculator").get()

    remoteCalculator << 1
    remoteCalculator << 2

    react { println it }
}

queryActor.join()

NettyTransportProvider.stopClients()