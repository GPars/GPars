package groovyx.gpars.remote

import groovyx.gpars.actor.Actors
import groovyx.gpars.remote.netty.NettyTransportProvider
import java.util.concurrent.TimeUnit

public abstract class SyncTest extends GroovyTestCase {
    void testDistSync() {
        def node1 = new LocalNode(new NettyTransportProvider(), {
            addDiscoveryListener {node, op ->
                if (op == "connected") {
                    def something = node.mainActor.sendAndWait("give me something")
                    assertEquals "here is something", something
                    node.mainActor.stop()
                    node.mainActor.join()
                    stop()
                }
            }

            loop { react {} }
        })

        def node2 = new LocalNode(new NettyTransportProvider(), {
            loop {
                react {msg ->
                    switch (msg) {
                        case "give me something":
                            reply "here is something"
                            break
                    }
                }
            }
        })

        node1.mainActor.join(5,TimeUnit.SECONDS)
        node1.localHost.disconnect()
        node2.localHost.disconnect()
    }

    void testSendAndContinue() {
        def a1 = Actors.actor {
            loop {
                react {msg ->
                    if (msg == "test") {
                        reply "so it goes"
                    }
                }
            }
        }

        def a2 = Actors.actor {
            loop {
                react {msg ->
                    if (msg == "test") {
                        reply(a1.sendAndWait("test"))
                    }
                }
            }
        }

        a2.sendAndContinue("test") {
            assertEquals "so it goes", it
            a1.stop()
            stop()
        }

        a1.join()
        a2.join()
    }
}