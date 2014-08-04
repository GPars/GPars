package groovyx.gpars.samples.remote.actor.calculator

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

def HOST = "localhost"
def PORT = 9000

def remoteActors = RemoteActors.create()

def queryActor = Actors.actor {
    println "Remote Calculator - Query"

    def remoteCalculator = remoteActors.get HOST, PORT, "remote-calculator" get()

    remoteCalculator << 1
    remoteCalculator << 2

    react { println it }
}

queryActor.join()