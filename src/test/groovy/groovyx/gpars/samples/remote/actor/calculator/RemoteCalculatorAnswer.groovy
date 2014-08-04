package groovyx.gpars.samples.remote.actor.calculator

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

def HOST = "localhost"
def PORT = 9000

def remoteActors = RemoteActors.create()
remoteActors.startServer HOST, PORT

def answerActor = Actors.actor {
    println "Remote Calculator - Answer"

    remoteActors.publish delegate, "remote-calculator"

    react { a->
        react { b->
            reply a + b
        }
    }
}

answerActor.join()

remoteActors.stopServer()
