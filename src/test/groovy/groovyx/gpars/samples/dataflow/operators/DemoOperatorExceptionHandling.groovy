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
 * Shows how to monitor operator's exceptions and react to them in listeners
 *
 * @author Vaclav Pech
 */

final group = new DefaultPGroup()

//When no listeners are registered, terminate upon exception
DataflowQueue a = new DataflowQueue()
DataflowQueue b = new DataflowQueue()
DataflowQueue c = new DataflowQueue()
def op = group.operator(inputs: [a, b], outputs: [c]) {x, y ->
    if (x < 0) throw new IllegalArgumentException("I do not like negative xs")
    bindOutput x + y
}

a << -1
b << 10
op.join()

//When all listener return false, the operator will not terminate
a = new DataflowQueue()
b = new DataflowQueue()
c = new DataflowQueue()
def listener = new DataflowEventAdapter() {
    @Override
    boolean onException(final DataflowProcessor processor, final Throwable e) {
        return false
    }
}

op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
    if (x < 0) throw new IllegalArgumentException("I do not like negative xs")
    bindOutput x + y
}

a << -1
b << 20
//No output, the exception was consumed up
a << 1
b << 2
assert 3 == c.val

op.terminate()

//When a single listener returns true, the operator will terminate
a = new DataflowQueue()
b = new DataflowQueue()
c = new DataflowQueue()
final listener1 = new DataflowEventAdapter() {
    @Override
    boolean onException(final DataflowProcessor processor, final Throwable e) {
        return false
    }
}

final listener2 = new DataflowEventAdapter() {
    @Override
    boolean onException(final DataflowProcessor processor, final Throwable e) {
        return true
    }
}

op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener1, listener2]) {x, y ->
    if (x < 0) throw new IllegalArgumentException("I do not like negative xs")
    bindOutput x + y
}

a << 10
b << 20
assert 30 == c.val

a << -1
b << 2
op.join()
assert !c.bound

group.shutdown()