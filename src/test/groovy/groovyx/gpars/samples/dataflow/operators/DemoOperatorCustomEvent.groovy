// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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
import groovyx.gpars.dataflow.operator.DataflowEventAdapter
import groovyx.gpars.dataflow.operator.DataflowProcessor
import groovyx.gpars.group.DefaultPGroup

/**
 * Shows how to use custom events to trigger custom event handlers.
 * In our example the operator reports through a custom event whenever the sum becomes too high. The listeners have
 * a chance to lower the result value that gets output from the operator.
 *
 * @author Vaclav Pech
 */

final group = new DefaultPGroup()
final DataflowQueue a = new DataflowQueue()
final DataflowQueue b = new DataflowQueue()
final DataflowQueue c = new DataflowQueue()

//The listener to monitor the operator
final listener = new DataflowEventAdapter() {
    @Override
    Object customEvent(DataflowProcessor processor, Object data) {
        println "Log: Getting quite high on the scale $data"
        return 100  //The value to use instead
    }
}

op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
    final sum = x + y
    if (sum > 100) bindOutput(fireCustomEvent(sum))  //Reporting that the sum is too high, binding the lowered value that comes back
    else bindOutput sum
}

a << 10
b << 20
assert 30 == c.val

a << 101
b << 2
assert 100 == c.val

op.terminate()
group.shutdown()