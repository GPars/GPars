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

/**
 * Calculates Fibonacci numbers using dataflow a operator.
 * The output of the operator is wired back to its input and one of the channels has a one-off delay.
 *
 * @author Vaclav Pech
 */
import static groovyx.gpars.dataflow.Dataflow.operator

final DataflowQueue ch1 = new DataflowQueue()
final DataflowQueue ch2 = new DataflowQueue()
final DataflowQueue ch3 = new DataflowQueue()

ch1 << 1
ch2 << 0
ch2 << 0

final op = operator([ch1, ch2], [ch3, ch1, ch2]) { a, b ->
    bindAllOutputs a + b
}


30.times {
    println ch3.val
}

op.terminateAfterNextRun()
