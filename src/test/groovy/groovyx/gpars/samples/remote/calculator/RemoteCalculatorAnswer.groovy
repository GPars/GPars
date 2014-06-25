package groovyx.gpars.samples.remote.calculator

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9000

NettyTransportProvider.startServer(HOST, PORT)

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

NettyTransportProvider.stopServer()
