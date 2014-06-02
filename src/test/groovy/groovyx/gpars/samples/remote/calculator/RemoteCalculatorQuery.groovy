package groovyx.gpars.samples.remote.calculator

import groovyx.gpars.actor.Actors
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.ClientNettyTransportProvider

import java.util.concurrent.CountDownLatch;

println "Remote Calculator (Query)"

def transport = new ClientNettyTransportProvider("localhost", 9000)

def mainNode = new LocalNode(transport, {
    println "Hi, I am $id"

    def latch = new CountDownLatch(1)

    def calculator

    addDiscoveryListener {node, operation ->
        System.err.println "${node.id} $operation"
        calculator = node.mainActor
        latch.countDown()
    }

    latch.await()
    calculator << 1
    calculator << 2

    react {result ->
        println result
    }
}, null)

mainNode.mainActor.join()
transport.disconnect()