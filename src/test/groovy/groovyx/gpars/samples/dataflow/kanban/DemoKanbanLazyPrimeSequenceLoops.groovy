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

import groovyx.gpars.dataflow.KanbanFlow
import groovyx.gpars.dataflow.KanbanLink
import groovyx.gpars.dataflow.KanbanTray
import static groovyx.gpars.dataflow.ProcessingNode.node
import java.util.concurrent.CountDownLatch
import groovyx.gpars.dataflow.ProcessingNode

/*
When a ProcessingNode is linked onto itself, it can avoid internal state
by exporting it as the product that it sends onto itself.
With this approach it is possible to provide state-free nodes
that serve as e.g. generators or collectors.
*/

def loopLink(KanbanFlow flow, ProcessingNode node, start){
    flow.cycleAllowed = true
    KanbanLink link = flow.link(node).to(node)
    link.downstream << new KanbanTray(link: link, product: start)
}

def flow = new KanbanFlow()

def counter = node { self, loop, down ->
    def val = self.take() + 1; loop val
    down val
}
def primes = node { up, down ->
    def x = up.take()
    if ( (2 ..< x ).any { y -> x % y == 0 } ) { // y is a factor of x => x is not prime
        ~down
    } else {
        down x
    }
}
def latch = new CountDownLatch(10) // stop eventually for demo reasons
def collector = node { self, loop, up ->
    latch.countDown()
    def col = self.take()
    println col                 // reveal the collected state for demo reasons
    loop (col + up.take())
}

loopLink flow, counter, 0       // self-cycle, number generator
loopLink flow, collector, []    // self-cycle, number collector
flow.link(counter).to(primes)   // filter for prime numbers in the middle
flow.link(primes).to(collector)

flow.start()
latch.await()
flow.stop()