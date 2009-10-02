package org.gparallelizer.remote

import org.gparallelizer.actor.Actors
import org.gparallelizer.remote.netty.NettyTransportProvider

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

    node1.mainActor.join()
    node1.localHost.disconnect()
    node2.localHost.disconnect()
  }

    //todo enable
  void _testSendAndContinue() {
    def a1 = Actors.actor {
      loop {
        react {msg ->
          if (msg == "test") {
            reply "so it goes"
          }
        }
      }
    }.start()

    def a2 = Actors.actor {
      loop {
        react {msg ->
          if (msg == "test") {
            reply(a1.sendAndWait("test"))
          }
        }
      }
    }.start()

    a2.sendAndContinue("test") {
      assertEquals "so it goes", it
      stop()
      a1.stop()
    }

    a1.join()
    a2.join()
  }
}