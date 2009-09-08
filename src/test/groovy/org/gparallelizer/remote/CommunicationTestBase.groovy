//  GParallelizer
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

package org.gparallelizer.remote

import java.util.concurrent.CountDownLatch

public abstract class CommunicationTestBase extends GroovyTestCase{

  RemoteTransportProvider getTransportProvider () {}

  void testDiscovery () {
      def node1 = new LocalNode(transportProvider)
      def node2 = new LocalNode(transportProvider)
      def node3 = new LocalNode(transportProvider)

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
        nodes[id] = new LocalNode(transportProvider, {
          addDiscoveryListener { n, op ->
            try {
              def msg = "${op == 'connected' ? 'Hi' : 'Bye'}, from $id"
              println "sending $msg"
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
                println "received $id: $msg"
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
