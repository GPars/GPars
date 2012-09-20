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
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.dataflow.operator.DataflowEventListener
import groovyx.gpars.dataflow.operator.DataflowProcessor
import groovyx.gpars.group.DefaultPGroup

/**
 * Shows all the lifecycle methods of dataflow operators and selectors
 *
 * @author Vaclav Pech
 */

final group = new DefaultPGroup()
final DataflowQueue a = new DataflowQueue()
final DataflowQueue b = new DataflowQueue()
final DataflowQueue c = new DataflowQueue()

//The listener to monitor the operator's lifecycle
final listener = new DataflowEventListener() {
    @Override
    void registered(final DataflowProcessor processor) {
        println "Hooked into an operator"
    }

    @Override
    void afterStart(final DataflowProcessor processor) {
        println 'afterStart'
    }

    @Override
    void afterStop(final DataflowProcessor processor) {
        println 'afterStop'
    }

    @Override
    boolean onException(final DataflowProcessor processor, final Throwable e) {
        println 'onException'
        processor.bindOutput 0      //Generate output for the incorrect input
        return false                //Do not terminate the operator
    }

    @Override
    Object messageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
        println 'messageArrived'
        return message
    }

    @Override
    Object controlMessageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
        println 'controlMessageArrived'
        return message
    }

    @Override
    Object messageSentOut(final DataflowProcessor processor, final DataflowWriteChannel<Object> channel, final int index, final Object message) {
        println 'messageSentOut'
        return message
    }

    @Override
    List<Object> beforeRun(final DataflowProcessor processor, final List<Object> messages) {
        println 'beforeRun'
        return messages
    }

    @Override
    void afterRun(final DataflowProcessor processor, final List<Object> messages) {
        println 'afterRun'
    }

    @Override
    Object customEvent(DataflowProcessor processor, Object data) {
        println 'customEvent'
    }
}

def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
    if (x < 0) throw new IllegalArgumentException('Do not want to handle negative values')
    bindOutput x + y
}

a << 10
b << 20
assert 30 == c.val
a << -1
b << 2
assert 0 == c.val

op.terminate()
group.shutdown()
