package org.gparallelizer.remote

import static org.gparallelizer.actors.pooledActors.PooledActors.*
import org.gparallelizer.dataflow.DataFlowVariable
import java.util.concurrent.CountDownLatch
import org.gparallelizer.remote.nonsharedmemory.NonSharedMemoryNode;

public abstract class CommunicationTestBase extends GroovyTestCase{
    void testShared () {
      def node1 = new LocalNode()
      def node2 = new LocalNode()
      def node3 = new LocalNode()

      def res = [:]
      def nodes = [node1, node2, node3]

      def latch
      latch = new CountDownLatch (nodes.size()*(nodes.size()-1))

      nodes.each { node ->
         def info = res [node.id] = [connected:[], disconnected:[]]
         node.addDiscoveryListener { anotherNode, op ->
           synchronized (info) {
               println "$node $op $anotherNode"
               info[op] << anotherNode.id
               latch.countDown ()
           }
         }
         node.connect ()
      }

      latch.await ()

      assertEquals ([node2.id, node3.id] as SortedSet, res[node1.id].connected as SortedSet)
      assertEquals ([node1.id, node3.id] as SortedSet, res[node2.id].connected as SortedSet)
      assertEquals ([node1.id, node2.id] as SortedSet, res[node3.id].connected as SortedSet) 

      latch = new CountDownLatch (nodes.size()*(nodes.size()-1))

      nodes.each { id ->
        id.disconnect ()
      }

      latch.await ()

      assertEquals ([node2.id, node3.id] as SortedSet, res[node1.id].disconnected as SortedSet)
      assertEquals ([node1.id, node3.id] as SortedSet, res[node2.id].disconnected as SortedSet)
      assertEquals ([node1.id, node2.id] as SortedSet, res[node3.id].disconnected as SortedSet)

      res.each { k, v -> println "$k : $v"}
    }

    void testMainActor () {
      def latch, latch2
      latch = new CountDownLatch (12)
      latch2 = new CountDownLatch (24)

      def nodes = [:]
      (0..3).each { id ->
        nodes[id] = new LocalNode({
          addDiscoveryListener { n, op ->
            try {
              def msg = "${op == 'connected' ? 'Hi' : 'Bye'}, from $id"
              println msg
              n.mainActor << msg
            }
            catch (Throwable t) {
              t.printStackTrace ()
            }
            finally {
              latch.countDown ()
            }
          }

          loop {
            react { msg ->
              try {
                println "To $id: $msg"
              }
              catch (Throwable t) {
                t.printStackTrace ()
              }
              finally {
                latch2.countDown ()
              }
            }
          }
        })
      }

      latch.await ()
      println "Discovery finished"

      latch = new CountDownLatch (12)

      (0..3).each { id ->
        nodes[id].disconnect ()
      }

      latch.await ()
      latch2.await ()
    }
}