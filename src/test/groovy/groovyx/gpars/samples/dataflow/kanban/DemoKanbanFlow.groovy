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
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.ResizeablePool
import java.util.concurrent.atomic.AtomicInteger
import static groovyx.gpars.dataflow.ProcessingNode.node

/**
 * A -> B wire simple flow with one producer and one consumer with a custom pooled group
 */

AtomicInteger count = new AtomicInteger(0) // needed to create some products
DataflowQueue results = new DataflowQueue()  // needed to assert collected products
KanbanFlow flow = new KanbanFlow()

def counter = { below -> below count.andIncrement }
def reporter = { above -> results << above.take() }

def producer = node(counter)
def consumer = node(reporter)
flow.pooledGroup = new DefaultPGroup(new ResizeablePool(true, 1))
flow.link(producer).to(consumer)

flow.start(1)

println 'Generated: ' + results.val
println 'Generated: ' + results.val
flow.stop()
