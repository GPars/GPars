package org.gparallelizer.remote

import org.gparallelizer.scheduler.Scheduler
import static org.gparallelizer.actors.pooledActors.PooledActors.*;

public class SharedMemoryTest extends GroovyTestCase {
    void testShared () {
      def scheduler = new Scheduler()

      def node1 = new LocalNode(scheduler)
      def node2 = new LocalNode(scheduler)
      def node3 = new LocalNode(scheduler)

      def res = [:]
      def nodes = [node1, node2, node3]

      nodes.each {
         res [it] = [connected:[], disconnected:[]]
         it.addDiscoveryListener { node, op ->
           println "$it $op $node"
           res.get(it)[op].add(node)
         }
         it.connect ()
      }

      nodes.each { it.disconnect () }

      Thread.currentThread().sleep 500
      
      scheduler.shutdown ()

      res.each { k, v -> println "$k : $v"}
    }

    void testMainActor () {
      def scheduler = new Scheduler()

      def nodes = [:]
      (0..5).each { id ->
        nodes[id] = new LocalNode(scheduler,{
          addDiscoveryListener { n, op ->
            n.mainActor << "Hi, from $id"
          }

          loop {
            react { msg ->
              println "$id: $msg"
            }
          }
        })
      }

      Thread.currentThread().sleep 500

      (0..5).each { id ->
        nodes[id].disconnect ()
      }
    }
}