package groovyx.gpars.samples.remote.calculator

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.NettyTransportProvider;

def answerActor = Actors.actor {
    println "Remote Calculator - Answer"

    RemoteActors.register(delegate)

    react { a->
        react { b->
            reply a + b
        }
    }
}

answerActor.join()
