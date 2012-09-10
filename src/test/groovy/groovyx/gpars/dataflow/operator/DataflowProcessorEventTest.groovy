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

import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class DataflowProcessorEventTest extends GroovyTestCase {

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

    public void testCustomEvent() {
        final listener1 = new TestListener()
        final listener2 = new TestListener()
        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener1, listener2]) {x, y ->
            final xa = fireCustomEvent(x)
            bindOutput xa + y
        }
        assert 0 == listener1.countEventsThatStartWith('customEvent')
        assert 0 == listener2.countEventsThatStartWith('customEvent')
        a << 10
        b << 20
        assert 60 == c.val
        assert 1 == listener1.countEventsThatStartWith('customEvent')
        assert 1 == listener2.countEventsThatStartWith('customEvent')
        assert "customEvent:10" == listener1.retrieveEvents {it.startsWith 'customEvent'}[0]
        assert "customEvent:20" == listener2.retrieveEvents {it.startsWith 'customEvent'}[0]
        a << 1
        b << 2
        assert 6 == c.val
        assert 2 == listener1.countEventsThatStartWith('customEvent')
        assert 2 == listener2.countEventsThatStartWith('customEvent')
        assert "customEvent:10" == listener1.retrieveEvents {it.startsWith 'customEvent'}[0]
        assert "customEvent:1" == listener1.retrieveEvents {it.startsWith 'customEvent'}[1]
        assert "customEvent:20" == listener2.retrieveEvents {it.startsWith 'customEvent'}[0]
        assert "customEvent:2" == listener2.retrieveEvents {it.startsWith 'customEvent'}[1]

        op.terminate()
    }

    public void testAfterStartAndStop() {
        final listener1 = new TestListener()
        final listener2 = new TestListener()

        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener1, listener2]) {x, y ->
            bindOutput x + y
        }

        a << 10
        b << 20
        assert 30 == c.val
        assert listener1.events[0].startsWith('registered')
        assert listener1.events[1].startsWith('afterStart')
        assert listener2.events[0].startsWith('registered')
        assert listener2.events[1].startsWith('afterStart')
        assert 1 == listener1.countEventsThatStartWith('afterStart')
        assert 1 == listener2.countEventsThatStartWith('afterStart')
        assert 0 == listener1.countEventsThatStartWith('afterStop')
        assert 0 == listener2.countEventsThatStartWith('afterStop')

        a << 1
        b << 2
        assert 3 == c.val
        assert listener1.events[1].startsWith('afterStart')
        assert listener2.events[1].startsWith('afterStart')
        assert 1 == listener1.countEventsThatStartWith('afterStart')
        assert 1 == listener2.countEventsThatStartWith('afterStart')
        assert 0 == listener1.countEventsThatStartWith('afterStop')
        assert 0 == listener2.countEventsThatStartWith('afterStop')

        op.terminate()
        op.join()
        assert listener1.events.last().startsWith('afterStop')
        assert listener2.events.last().startsWith('afterStop')
        assert 1 == listener1.countEventsThatStartWith('afterStart')
        assert 1 == listener2.countEventsThatStartWith('afterStart')
        assert 1 == listener1.countEventsThatStartWith('afterStop')
        assert 1 == listener2.countEventsThatStartWith('afterStop')
    }

    public void testOperatorMessages() {
        final listener1 = new TestListener()
        final listener2 = new TestListener()

        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener1, listener2]) {x, y ->
            bindOutput x + y
        }

        a << 10
        b << 20
        assert 30 == c.val
        assert 2 == listener1.countEventsThatStartWith('messageArrived')
        assert 2 == listener2.countEventsThatStartWith('messageArrived')
        assert 1 == listener1.countEventsThatStartWith('beforeRun')
        assert 1 == listener2.countEventsThatStartWith('beforeRun')
        assert 1 == listener1.countEventsThatStartWith('messageSentOut')
        assert 1 == listener2.countEventsThatStartWith('messageSentOut')
        assert 1 >= listener1.countEventsThatStartWith('afterRun')
        assert 1 >= listener2.countEventsThatStartWith('afterRun')

        final arrived1 = listener1.retrieveEvents {it.startsWith 'messageArrived'}
        assert 'messageArrived 10' == arrived1.first()
        assert 'messageArrived 20' == arrived1.last()
        assert 'messageSentOut 30' == listener1.retrieveEvents {it.startsWith 'messageSentOut'}.first()

        a << 1
        b << 2
        assert 3 == c.val
        final arrived2 = listener2.retrieveEvents {it.startsWith 'messageArrived'}
        assert 'messageArrived 1' == arrived2[2]
        assert 'messageArrived 2' == arrived2.last()
        assert 'messageSentOut 30' == listener1.retrieveEvents {it.startsWith 'messageSentOut'}.first()
        assert 'messageSentOut 3' == listener1.retrieveEvents {it.startsWith 'messageSentOut'}.last()

        op.terminate()
    }

    public void testSelectorMessages() {
        final listener1 = new TestListener()
        final listener2 = new TestListener()

        def op = group.selector(inputs: [a, b], outputs: [c], listeners: [listener1, listener2]) {x ->
            bindOutput x
        }

        a << 10
        assert 10 == c.val
        b << 20
        assert 20 == c.val
        assert 2 == listener1.countEventsThatStartWith('messageArrived')
        assert 2 == listener2.countEventsThatStartWith('messageArrived')
        assert 2 == listener1.countEventsThatStartWith('beforeRun')
        assert 2 == listener2.countEventsThatStartWith('beforeRun')
        assert 2 == listener1.countEventsThatStartWith('messageSentOut')
        assert 2 == listener2.countEventsThatStartWith('messageSentOut')
        assert 2 >= listener1.countEventsThatStartWith('afterRun')
        assert 2 >= listener2.countEventsThatStartWith('afterRun')

        final arrived1 = listener1.retrieveEvents {it.startsWith 'messageArrived'}
        assert 'messageArrived 10' == arrived1.first()
        assert 'messageArrived 20' == arrived1.last()
        assert 'messageSentOut 10' == listener1.retrieveEvents {it.startsWith 'messageSentOut'}.first()
        assert 'messageSentOut 20' == listener1.retrieveEvents {it.startsWith 'messageSentOut'}.last()

        op.terminate()
    }

    public void testOperatorControlMessages() {
        final listener1 = new TestListener()
        final listener2 = new TestListener()

        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener1, listener2]) {x, y ->
            bindOutput x + y
        }

        a << PoisonPill.instance
        assert PoisonPill.instance == c.val
        assert 1 == listener1.countEventsThatStartWith('controlMessageArrived')
        assert 1 == listener2.countEventsThatStartWith('controlMessageArrived')
        assert 0 == listener1.countEventsThatStartWith('messageArrived')
        assert 0 == listener2.countEventsThatStartWith('messageArrived')
        assert 0 == listener1.countEventsThatStartWith('beforeRun')
        assert 0 == listener2.countEventsThatStartWith('beforeRun')
        assert 1 == listener1.countEventsThatStartWith('messageSentOut')
        assert 1 == listener2.countEventsThatStartWith('messageSentOut')
        assert 0 >= listener1.countEventsThatStartWith('afterRun')
        assert 0 >= listener2.countEventsThatStartWith('afterRun')

        op.terminate()
    }

    public void testSelectorControlMessages() {
        final listener1 = new TestListener()
        final listener2 = new TestListener()

        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener1, listener2]) {x, y ->
            bindOutput x + y
        }

        a << PoisonPill.instance
        assert PoisonPill.instance == c.val
        assert 1 == listener1.countEventsThatStartWith('controlMessageArrived')
        assert 1 == listener2.countEventsThatStartWith('controlMessageArrived')
        assert 0 == listener1.countEventsThatStartWith('messageArrived')
        assert 0 == listener2.countEventsThatStartWith('messageArrived')
        assert 0 == listener1.countEventsThatStartWith('beforeRun')
        assert 0 == listener2.countEventsThatStartWith('beforeRun')
        assert 1 == listener1.countEventsThatStartWith('messageSentOut')
        assert 1 == listener2.countEventsThatStartWith('messageSentOut')
        assert 0 >= listener1.countEventsThatStartWith('afterRun')
        assert 0 >= listener2.countEventsThatStartWith('afterRun')

        op.terminate()
    }

    class TestListener implements DataflowEventListener {
        volatile CopyOnWriteArrayList<String> events = []

        List<String> retrieveEvents(Closure filter) {
            events.findAll filter
        }

        int countEventsThatStartWith(String filter) {
            retrieveEvents {it.startsWith(filter)}.size()
        }

        @Override
        void registered(final DataflowProcessor processor) {
            events << 'registered'
        }

        @Override
        void afterStart(final DataflowProcessor processor) {
            events << 'afterStart'
        }

        @Override
        void afterStop(final DataflowProcessor processor) {
            events << 'afterStop'
        }

        @Override
        boolean onException(final DataflowProcessor processor, final Throwable e) {
            events << 'onException'
            false
        }

        @Override
        Object messageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
            events << 'messageArrived ' + String.valueOf(message)
            message
        }

        @Override
        Object controlMessageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
            events << 'controlMessageArrived ' + String.valueOf(message)
            return message
        }

        @Override
        Object messageSentOut(final DataflowProcessor processor, final DataflowWriteChannel<Object> channel, final int index, final Object message) {
            events << 'messageSentOut ' + String.valueOf(message)
            return message
        }

        @Override
        List<Object> beforeRun(final DataflowProcessor processor, final List<Object> messages) {
            events << 'beforeRun'
            return messages
        }

        @Override
        void afterRun(final DataflowProcessor processor, final List<Object> messages) {
            events << 'afterRun'
        }

        @Override
        Object customEvent(DataflowProcessor processor, Object data) {
            events << 'customEvent:' + data
            data * 2
        }
    }
}
