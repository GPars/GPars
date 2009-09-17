package org.gparallelizer.dataflow

import org.gparallelizer.remote.LocalNode
import org.gparallelizer.remote.netty.NettyTransportProvider

public class DistributedDataFlowTest extends GroovyTestCase {
  void testDF() {
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
                msg.dataFlow.whenBound {v ->
                  println v
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
      it.mainActor.join()
      it.localHost.disconnect()
    }
  }
}