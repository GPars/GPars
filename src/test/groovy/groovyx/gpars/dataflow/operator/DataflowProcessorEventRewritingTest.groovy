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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup

/**
 * @author Vaclav Pech
 */
class DataflowProcessorEventRewritingTest extends GroovyTestCase {

    private PGroup group
    final DataflowQueue a = new DataflowQueue()
    final DataflowQueue b = new DataflowQueue()
    final DataflowQueue c = new DataflowQueue()

    protected void setUp() {
        group = new DefaultPGroup(1)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }

    public void testOperatorMessages() {
        final listener1 = new RewritingTestListener()
        final listener2 = new RewritingTestListener()

        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener1, listener2]) {x, y ->
            bindOutput x + y
        }

        a << 10
        b << 20
        assert "messageSentOut:" * 2 + "beforeRun:" * 2 + "messageArrived:" * 2 + 10 + "beforeRun:" * 2 + "messageArrived:" * 2 + 20 == c.val

        a << PoisonPill.instance
        b << 20
        assert "messageSentOut:" * 2 + "beforeRun:" * 2 + "messageArrived:" * 2 + "controlMessageArrived:PoisonPill" + "beforeRun:" * 2 + "messageArrived:" * 2 + 20 == c.val

        op.terminate()
    }

    public void testSelectorMessages() {
        final listener1 = new RewritingTestListener()
        final listener2 = new RewritingTestListener()

        def op = group.selector(inputs: [a, b], outputs: [c], listeners: [listener1, listener2]) {x ->
            bindOutput x
        }

        a << 10
        assert "messageSentOut:" * 2 + "beforeRun:" * 2 + "messageArrived:" * 2 + 10 == c.val

        a << PoisonPill.instance
        assert "messageSentOut:" * 2 + "beforeRun:" * 2 + "messageArrived:" * 2 + "controlMessageArrived:PoisonPill" == c.val

        op.terminate()
    }

    class RewritingTestListener extends DataflowEventAdapter {

        @Override
        Object messageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
            "messageArrived:" + message
        }

        @Override
        Object controlMessageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
            return "controlMessageArrived:PoisonPill"
        }

        @Override
        Object messageSentOut(final DataflowProcessor processor, final DataflowWriteChannel<Object> channel, final int index, final Object message) {
            return "messageSentOut:" + message
        }

        @Override
        List<Object> beforeRun(final DataflowProcessor processor, final List<Object> messages) {
            return messages.collect {"beforeRun:" + it}
        }
    }
}
