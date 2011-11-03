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

package groovyx.gpars.samples.dataflow.kanban

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.KanbanFlow
import java.util.concurrent.atomic.AtomicInteger
import static groovyx.gpars.dataflow.ProcessingNode.node

/**
 * A -> B,C flow with one producer and two consumers(broadcast)
 */

AtomicInteger count = new AtomicInteger(0) // needed to create some products
DataflowQueue results = new DataflowQueue()  // needed to assert collected products
KanbanFlow flow = new KanbanFlow()

def reporter = { above -> results << above.take() }

def producer = node { down1, down2 ->
    def val = count.andIncrement
    down1 << val
    down2 << val
}
def consumer1 = node(reporter)
def consumer2 = node(reporter)
flow.link(producer).to(consumer1)
flow.link(producer).to(consumer2)

flow.start()
4.times { println 'Generated: ' + results.val } // "worst" case of scheduling when one consumer is always faster
flow.stop()
