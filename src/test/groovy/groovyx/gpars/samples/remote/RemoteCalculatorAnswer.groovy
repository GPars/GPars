// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.remote;

import groovyx.gpars.actor.Actors
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.NettyTransportProvider

println "Calculator"

// transport provider communicating over IP
// it includes 4 functions in parallel
// - multicast of own identity
// - IP server accepting incoming connections from other nodes
// - multicast listener listening for broadcasts sent by other nodes
// - IP client connection to discovered nodes
def transport = new NettyTransportProvider("10.0.0.1")

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
            // let us remember who we connected with
            connected[node.id] = node
        }
        else {
            // disconnected, so log and forget it
            connected.remove node.id
        }
    }

    loop {
        react {a ->
		react { b ->
			println "${a+b}"
			reply a+b
            }
        }
    }
})

// let us wait before our application finished
mainNode.mainActor.join()
// now we can shutdown communication
transport.disconnect()
