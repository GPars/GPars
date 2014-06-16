package groovyx.gpars.samples.remote.calculator

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors

def answerActor = Actors.actor {
    println "Remote Calculator - Answer"

    RemoteActors.register(delegate, "remote-calculator")

    react { a->
        react { b->
            reply a + b
        }
    }
}

answerActor.join()
