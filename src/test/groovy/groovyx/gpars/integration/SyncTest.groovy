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

package groovyx.gpars.integration

import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.NettyTransportProvider
import java.util.concurrent.TimeUnit

public class SyncTest extends GroovyTestCase {
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

        node1.mainActor.join(5, TimeUnit.SECONDS)
        node1.localHost.disconnect()
        node2.localHost.disconnect()
    }

    void testSendAndContinue() {
        final def result = new DataFlowVariable()

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
            result << it
            a1.stop()
            stop()
        }

        a1.join()
        a2.join()

        assertEquals "so it goes", result.val
    }
}