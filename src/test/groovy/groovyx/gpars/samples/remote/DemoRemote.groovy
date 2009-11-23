package groovyx.gpars.samples.remote

import groovyx.gpars.actor.Actors
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.NettyTransportProvider

println """Welcome to chat!
Every line you will type will be printed on all JVM, which runs this script.

Type '@bye' to exit
Type '@kill <UUID of other node>' to stop it
Type '@kill all' to stop all other nodes
"""

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

    // define callback to be called when our node connected/diconnected with other nodes on the network
    addDiscoveryListener {node, operation ->
        System.err.println "${node.id} $operation"

        if (operation == "connected") {
            // every node (in this case RemoteNode, which is proxy to another LocalNode)
            // expose it's main actor
            // this actor can be used as main communication point
            // so here we send command to this distributed actor
            node.mainActor << [command: "print", line: welcome, id: id]

            // let us remember who we connected with
            connected.put(node.id, node)
        }
        else {
            // disconnected, so log and forget it
            connected.remove node.id
        }
    }

    // Parser for input stream
    // This is inner actor responsible for converting console input in to commands,
    // which our main actor understands
    Actors.actor {

        // as console input doesn't fit well to actor model we create
        // daemon thread to read standard input and feed parser
        Thread.startDaemon {
            def reader = new LineNumberReader(new InputStreamReader(System.in))
            while (!Thread.currentThread().isInterrupted()) {
                def line = reader.readLine()

                // magic Groovy way to access outer actor
                owner.delegate << line
            }
        }

        // magic way to access outer actor
        def mainActor = owner.delegate

        // Groovy magic allows us access id of LocalNode via main actor
        def id = mainActor.id

        // nothing really interesting inside the loop
        // transform input to commands sent to mainActor
        loop {
            react {line ->
                if (line == '@bye') {
                    mainActor.stop()
                    stop()
                    return
                }

                if (line.startsWith("@kill ")) {
                    def whom = line[6..-1]
                    if (whom == "all") {
                        mainActor << [command: "kill all", id: id]
                        return
                    }
                    else {
                        try {
                            def victim = UUID.fromString(whom)
                            mainActor << [command: "kill", victim: victim, id: id]
                            return
                        }
                        catch (IllegalArgumentException e) {
                            // something went wrong so let us just print
                        }
                    }
                    return
                }
                mainActor << [command: "broadcast", line: line, id: id]
            }
        }
    }

    // main loop of main actor starts here
    // the only interesting thing to notice is (and this is main point)
    // that we use main actor of another node exactly as if it was normal actor
    // in fact it is normal actor (implements Actor interface)
    loop {
        react {msg ->
            switch (msg.command) {
                case "broadcast":
                    connected.each {nid, n ->
                        n.mainActor << [command: "print", line: msg.line, id: id]
                    }
                    return

                case "kill all":
                    connected.each {nid, n ->
                        n.mainActor << [command: "die", id: id]
                    }
                    return

                case "kill":
                    connected[msg.victim].mainActor << [command: "die", id: id]
                    return


                case "print":
                    println "${msg.id} says ${msg.line}"
                    return

                case "die":
                    println "${msg.id} asked to stop"
                    delegate.stop()
                    return
            }
        }
    }
})

// let us wait before our application finished
mainNode.mainActor.join()
// now we can shutdown communication
transport.disconnect()