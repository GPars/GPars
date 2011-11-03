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
import groovyx.gpars.dataflow.KanbanLink
import groovyx.gpars.dataflow.KanbanTray
import static groovyx.gpars.dataflow.ProcessingNode.node

/**
 * A <-> A cycle: ping-ping self-contained counter (generator)
 */

DataflowQueue results = new DataflowQueue()  // needed to assert collected products

def a = node { up, dn -> def counterValue = up.take() + 1; results << counterValue; dn << counterValue }

KanbanFlow flow = new KanbanFlow()
flow.cycleAllowed = true
KanbanLink link = flow.link(a).to(a)

flow.start(1)
link.downstream << new KanbanTray(link: link, product: 0)

4.times { println 'Generated: ' + results.val }
flow.stop()