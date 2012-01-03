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

package groovyx.gpars.samples.dataflow.operators

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.operator.CountingPoisonPill
import groovyx.gpars.group.NonDaemonPGroup

/**
 * Dataflow operators and selectors can be stopped in two ways - calling the terminate() method on all operators that need to be stopped
 * or by sending a poisson message. This demo shows the second approach.
 * By using a CountingPoisonPill other threads can be waiting for a specified number of operators in the network to be terminated.
 * The termination property of the CountingPoisonPill class is a regular Promise<Boolean> and so has a lot of handy properties.
 *
 * After receiving a poisson an operator stops. It only makes sure the poisson is first sent to all its output channels, so that the poisson can spread
 * to the connected operators.
 */


final DataflowQueue a = new DataflowQueue()
final DataflowQueue b = new DataflowQueue()
final DataflowQueue c = new DataflowQueue()
final DataflowQueue d = new DataflowQueue()
final DataflowQueue e = new DataflowQueue()
final DataflowQueue f = new DataflowQueue()
final DataflowQueue out = new DataflowQueue()

final def group = new NonDaemonPGroup()

def op1 = group.operator(inputs: [a, b, c], outputs: [d, e]) {x, y, z -> }

def op2 = group.selector(inputs: [d], outputs: [f, out]) { }

def op3 = group.prioritySelector(inputs: [e, f], outputs: [b]) {value, index -> }

//Send the poisson indicating the number of operators than need to be terminated before we can continue
final pill = new CountingPoisonPill(3)
pill.termination.whenBound {println "Reporting asynchronously that the network has been stopped"}
a << pill

if (pill.termination.bound) println "Wow, that was quick. We are done already!"
else println "Things are being slow today. The network is still running."

//Wait for all operators to terminate
assert pill.termination.get()
//At least 3 operators should be terminated by now

assert out.val == pill  //The poisson will fall out from the output channels

println "All operators have stopped."
group.shutdown()
