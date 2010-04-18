// GPars (formerly GParallelizer)
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

package groovyx.gpars.dataflow

import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.NettyTransportProvider
import java.util.concurrent.TimeUnit

public abstract class DistributedDataFlowTest extends GroovyTestCase {
    void testDF() {
        final DataFlows df = new DataFlows()

        def results = [one: new DataFlowVariable(), two: new DataFlowVariable()]
        def nodes = ["one", "two"].collect {node ->
            new LocalNode(new NettyTransportProvider(), {
                addDiscoveryListener {anotherNode, op ->
                    if (op == "connected") {
                        delegate.send([command: "connected", actor: anotherNode.mainActor])
                    }
                }

                def dataFlow = new DataFlowVariable();

                loop {
                    react {msg ->
                        switch (msg.command) {

                            case "connected": // 1
                                msg.actor << [command: "getDataFlow", to: delegate]
                                break

                            case "getDataFlow":  // 2
                                msg.to << [command: "dataFlow", dataFlow: dataFlow, actor: delegate]
                                break

                            case "dataFlow":  // 1
                                msg.actor << [command: "setDataFlow", dataFlow: msg.dataFlow, value: node]
                                msg.dataFlow.whenNextBound {v ->
                                    df."$node" = v
                                }
                                msg.dataFlow << node
                                break

                            case "setDataFlow": // 2
                                results[node] << msg.dataFlow.val
                                break
                        }
                    }
                }
            })
        }

        results.one.whenBound {
            results.two.whenBound {
                nodes[0].mainActor.stop()
                nodes[1].mainActor.stop()
            }
        }

        nodes.each {
            it.mainActor.join(5, TimeUnit.SECONDS)
            it.localHost.disconnect()
        }
        assertEquals 'two', results.one.val
        assertEquals 'one', results.two.val
        assertEquals 'one', df['one']
        assertEquals 'two', df['two']
    }
}