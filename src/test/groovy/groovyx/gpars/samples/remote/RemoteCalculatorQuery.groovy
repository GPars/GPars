package groovyx.gpars.samples.remote

import groovyx.gpars.actor.Actors
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.NettyTransportProvider

println "Remote Calculator"

// transport provider communicating over IP
// it includes 4 functions in parallel
// - multicast of own identity
// - IP server accepting incoming connections from other nodes
// - multicast listener listening for broadcasts sent by other nodes
// - IP client connection to discovered nodes
def transport = new NettyTransportProvider()

// Here we start new distributed node communicating over IP
// and start actor on this node
// usually you have only one LocalNode per JVM but it is not must
def mainNode = new LocalNode(transport, {
    def connected = [:]

    // id - is property of LocalNode (random UUID)
    def welcome = "Hi, I am ${id}"

    println welcome

    // define callback to be called when our node connected/disconnected with other nodes on the network
    addDiscoveryListener {node, operation ->
        System.err.println "${node.id} $operation"

        if (operation == "connected") {
            // every node (in this case RemoteNode, which is proxy to another LocalNode)
            // expose it's main actor
            // this actor can be used as main communication point
            // so here we send command to this distributed actor
            node.mainActor << 1
            node.mainActor << 2

            // let us remember who we connected with
            connected[node.id] = node


        }
        else {
            // disconnected, so log and forget it
            connected.remove node.id
        }
    }

    // main loop of main actor starts here
    // the only interesting thing to notice is (and this is main point)
    // that we use main actor of another node exactly as if it was normal actor
    // in fact it is normal actor (implements Actor interface)

    react { ans ->
        println ans
    }
})

// let us wait before our application finished
mainNode.mainActor.join()
// now we can shutdown communication
transport.disconnect()
