package groovyx.gpars.samples.remote.calculator

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

def queryActor = Actors.actor {
    println "Remote Calculator - Query"

    def remoteCalculator = RemoteActors.get("localhost", 9000, "remote-calculator")

    remoteCalculator << 1
    remoteCalculator << 2

    react { println it }

    remoteCalculator.stop() // client never closes?
}

queryActor.join()

RemoteActors.shutdown()