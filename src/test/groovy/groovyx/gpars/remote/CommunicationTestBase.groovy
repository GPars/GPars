//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package groovyx.gpars.remote

import groovyx.gpars.actor.Actors
import java.util.concurrent.CountDownLatch

public abstract class CommunicationTestBase extends GroovyTestCase {

    LocalHost getTransportProvider() {}

    void testRemote() {
        def node1 = new LocalNode(transportProvider, {
            def printer = Actors.actor {
                loop {
                    react {msg ->
                        println "node1: $msg"
                    }
                }
            }

            loop {
                react {command ->
                    switch (command) {
                        case "getPrinter":
                            reply printer
                            return

                        case "stop":
                            printer.stop()
                            stop()
                            return
                    }
                }
            }
        })

        def node2 = new LocalNode(transportProvider, {
            addDiscoveryListener {anotherNode, op ->
                if (op == "connected") {
                    def printer = anotherNode.mainActor.sendAndWait("getPrinter")
                    println printer
                    printer << "Hi"
                    printer << "Bye"
                    anotherNode.mainActor << "stop"
                    stop()
                }
            }

            loop {
                react {

                }
            }
        })

        node1.mainActor.join()
        node1.localHost.disconnect()
        node2.localHost.disconnect()
    }

    void testDiscovery() {
        def node1 = new LocalNode(transportProvider)
        def node2 = new LocalNode(transportProvider)
        def node3 = new LocalNode(transportProvider)

        def res = [:]
        def nodes = [node1, node2, node3]

        def latch
        latch = new CountDownLatch(nodes.size() * (nodes.size() - 1))

        nodes.each {node ->
            def info = res[node.id] = [connected: [], disconnected: []]
            node.addDiscoveryListener {anotherNode, op ->
                synchronized (info) {
                    println "$node $op $anotherNode"
                    info[op] << anotherNode.id
                    latch.countDown()
                }
            }
            node.connect()
        }

        latch.await()

        assertEquals([node2.id, node3.id] as SortedSet, res[node1.id].connected as SortedSet)
        assertEquals([node1.id, node3.id] as SortedSet, res[node2.id].connected as SortedSet)
        assertEquals([node1.id, node2.id] as SortedSet, res[node3.id].connected as SortedSet)

        latch = new CountDownLatch(nodes.size() * (nodes.size() - 1))

        nodes.each {id ->
            id.disconnect()
        }

        latch.await()

        assertEquals([node2.id, node3.id] as SortedSet, res[node1.id].disconnected as SortedSet)
        assertEquals([node1.id, node3.id] as SortedSet, res[node2.id].disconnected as SortedSet)
        assertEquals([node1.id, node2.id] as SortedSet, res[node3.id].disconnected as SortedSet)

        res.each {k, v -> println "$k : $v"}
    }

    void testMainActor() {
        def connectDisconnectLatch = new CountDownLatch(24)
        def printLatch = new CountDownLatch(12)

        def nodes = [:]
        (0..3).each {id ->
            nodes[id] = new LocalNode(transportProvider, {
                addDiscoveryListener {n, op ->
                    try {
                        if (op == "connected") {
                            def msg = "Hi, from $id"
                            println "sending $msg"
                            n.mainActor << msg
                        }
                    }
                    catch (Throwable t) {
                        t.printStackTrace()
                    }
                    finally {
                        connectDisconnectLatch.countDown()
                    }
                }

                loop {
                    react {msg ->
                        try {
                            println "${Thread.currentThread().id} received $id: $msg"
                        }
                        catch (Throwable t) {
                            t.printStackTrace()
                        }
                        finally {
                            printLatch.countDown()
                        }
                    }
                }
            })
        }

        printLatch.await()

        (0..3).each {id ->
            nodes[id].disconnect()
        }

        connectDisconnectLatch.await()
    }
}
