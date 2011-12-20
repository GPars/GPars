// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Dataflows
import groovyx.gpars.remote.LocalNode
import groovyx.gpars.remote.netty.NettyTransportProvider
import java.util.concurrent.TimeUnit

public class DistributedDataflowTest extends GroovyTestCase {
    void testDF() {
        final Dataflows df = new Dataflows()

        def results = [one: new DataflowVariable(), two: new DataflowVariable()]
        def nodes = ["one", "two"].collect {node ->
            new LocalNode(new NettyTransportProvider(), {
                addDiscoveryListener {anotherNode, op ->
                    if (op == "connected") {
                        delegate.send([command: "connected", actor: anotherNode.mainActor])
                    }
                }

                def dataflow = new DataflowVariable();

                loop {
                    react {msg ->
                        switch (msg.command) {

                            case "connected": // 1
                                msg.actor << [command: "getDataflow", to: delegate]
                                break

                            case "getDataflow":  // 2
                                msg.to << [command: "dataflow", dataflow: dataflow, actor: delegate]
                                break

                            case "dataflow":  // 1
                                msg.actor << [command: "setDataflow", dataflow: msg.dataflow, value: node]
                                msg.dataflow.whenBound {v ->
                                    df."$node" = v
                                }
                                msg.dataflow << node
                                break

                            case "setDataflow": // 2
                                results[node] << msg.dataflow.val
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
        assert 'two' == results.one.val
        assert 'one' == results.two.val
        assert 'one' == df['one']
        assert 'two' == df['two']
    }
}