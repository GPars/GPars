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
 * Shows how to leverage dataflow events to manage external resources
 *
 * @author Vaclav Pech
 */

final group = new DefaultPGroup()
final DataflowQueue a = new DataflowQueue()
final DataflowQueue b = new DataflowQueue()
final DataflowQueue c = new DataflowQueue()

//The object to hold operator's state
final state = [:]

//The listener to monitor the operator's lifecycle
final listener = new DataflowEventAdapter() {
    final data = new ByteArrayOutputStream()

    @Override
    void afterStart(final DataflowProcessor processor) {
        //Initializing the resource
        final log = new PrintWriter(new OutputStreamWriter(data))
        state.log = log
    }

    @Override
    void afterStop(final DataflowProcessor processor) {
        //Closing the resource
        state.log.flush()
        state.log.close()
    }
}

def op = group.operator(inputs: [a, b], outputs: [c], stateObject: state, listeners: [listener]) {x, y ->
    stateObject.log.print("Processing $x and $y;")
    bindOutput x + y
}

a << 10
b << 20
assert 30 == c.val
a << 1
b << 2
assert 3 == c.val

op.terminate()
group.shutdown()
assert """Processing 10 and 20;Processing 1 and 2;""" == listener.data.toString()