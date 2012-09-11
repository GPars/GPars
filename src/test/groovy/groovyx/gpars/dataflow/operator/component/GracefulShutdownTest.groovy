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

package groovyx.gpars.dataflow.operator.component

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup
import groovyx.gpars.dataflow.operator.DataflowProcessor
import groovyx.gpars.dataflow.DataflowReadChannel
import java.util.concurrent.CyclicBarrier
import groovyx.gpars.dataflow.DataflowBroadcast

/**
 * @author Vaclav Pech
 */
public class GracefulShutdownTest extends GroovyTestCase {
    private PGroup group
    final DataflowQueue a = new DataflowQueue()
    final DataflowQueue b = new DataflowQueue()
    final DataflowQueue c = new DataflowQueue()

    protected void setUp() {
        group = new DefaultPGroup(10)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }


    public void testSingleOperatorShutdown() throws Exception {
        final monitor = new GracefulShutdownMonitor();
        final listener = new GracefulShutdownListener(monitor)
        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
            bindOutput x + y
        }

        a << 10
        b << 20
        assert 30 == c.val

        final shutdownPromise = monitor.shutdownNetwork()
        shutdownPromise.get()
        op.join()

        sleep(500)
        assert !c.bound
    }

    public void testArrivedMessagePreventsShutdown() throws Exception {
        final monitor = new GracefulShutdownMonitor();

        final barrier = new CyclicBarrier(2)
        final listener = new GracefulShutdownListener(monitor) {
            @Override
            Object messageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
                final arrived = super.messageArrived(processor, channel, index, message)
                barrier.await()
                return arrived
            }
        }
        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
            throw new IllegalStateException("Should never get here")
        }
        a << 10
        barrier.await()
        assert !listener.isIdle()
        assert !listener.isIdleAndNoIncomingMessages()

        op.terminate()
    }

    public void testNonEmptyChannelPreventsShutdown() throws Exception {
        final monitor = new GracefulShutdownMonitor();

        final barrier1 = new CyclicBarrier(2)
        final barrier2 = new CyclicBarrier(2)

        final listener = new GracefulShutdownListener(monitor) {
            @Override
            void afterRun(final DataflowProcessor processor, final List<Object> messages) {
                super.afterRun(processor, messages)
                barrier1.await()
                barrier2.await()
            }
        }

        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
            bindOutput x + y
        }

        a << 10
        a << 100
        b << 20
        assert 30 == c.val

        barrier1.await()
        assert listener.isIdle()
        assert !listener.isIdleAndNoIncomingMessages()
        barrier2.await()
        b << 200

        monitor.shutdownNetwork().get()
        assert c.val == 300
    }

    public void testNonQueuedNonArrivedMessagePreventsShutdown() throws Exception {
        final monitor = new GracefulShutdownMonitor();

        final barrier1 = new CyclicBarrier(2)
        final barrier2 = new CyclicBarrier(2)

        final listener = new GracefulShutdownListener(monitor) {
            @Override
            Object messageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
                barrier1.await()
                barrier2.await()
                return super.messageArrived(processor, channel, index, message)
            }
        }

        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
            bindOutput x + y
        }

        a << 10
        a << 100
        b << 20
        barrier1.await()  //for message 10
        barrier2.await()
        barrier1.await()  //for message 20
        barrier2.await()
        assert 30 == c.val

        barrier1.await()  //for message 100

        assert listener.isIdle()
        assert !listener.isIdleAndNoIncomingMessages()
        barrier2.await()

        b << 200
        barrier1.await()  //for message 200
        barrier2.await()

        monitor.shutdownNetwork().get()
        assert 300 == c.val
    }

    public void testNonQueuedNonArrivedControlMessagePreventsShutdown() throws Exception {
        final monitor = new GracefulShutdownMonitor();

        final barrier1 = new CyclicBarrier(2)
        final barrier2 = new CyclicBarrier(2)

        final listener = new GracefulShutdownListener(monitor) {
            @Override
            Object controlMessageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
                barrier1.await()
                barrier2.await()
                return super.controlMessageArrived(processor, channel, index, message)
            }
        }

        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
            bindOutput x + y
        }

        a << 10
        a << new TestControlMessage()
        b << 20
        assert 30 == c.val

        barrier1.await()  //for test control message

        assert listener.isIdle()
        assert !listener.isIdleAndNoIncomingMessages()
        barrier2.await()

        monitor.shutdownNetwork().get()
    }

    public void testMultipleIdenticalMessagesThatPreventShutdown() throws Exception {
        final monitor = new GracefulShutdownMonitor();

        final barrier1 = new CyclicBarrier(2)
        final barrier2 = new CyclicBarrier(2)

        final listener = new GracefulShutdownListener(monitor) {
            @Override
            Object messageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
                barrier1.await()
                barrier2.await()
                return super.controlMessageArrived(processor, channel, index, message)
            }
        }

        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
            bindOutput x + y
        }

        a << 10
        a << 10
        b << 20
        barrier1.await()  //for message 10
        barrier2.await()
        barrier1.await()  //for message 20
        barrier2.await()
        assert 30 == c.val

        barrier1.await()  //for test control message

        assert listener.isIdle()
        assert !listener.isIdleAndNoIncomingMessages()
        barrier2.await()

        monitor.shutdownNetwork().get()
    }

    public void testSlowSingleOperatorShutdown() throws Exception {
        final monitor = new GracefulShutdownMonitor(100);
        final listener = new GracefulShutdownListener(monitor)
        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
            sleep 50
            bindOutput x + y
        }

        100.times{a << 10}
        100.times{b << 20}

        final shutdownPromise = monitor.shutdownNetwork()

        100.times{assert 30 == c.val}
        shutdownPromise.get()
        op.join()

        sleep(500)
        assert !c.bound
    }

    public void testGracefulOperatorShutdown() throws Exception {
        10.times {
            final DataflowQueue a = new DataflowQueue()
            final DataflowQueue b = new DataflowQueue()
            final DataflowQueue c = new DataflowQueue()
            final d = new DataflowQueue<Object>()
            final e = new DataflowBroadcast<Object>()
            final f = new DataflowQueue<Object>()
            final result = new DataflowQueue<Object>()

            final monitor = new GracefulShutdownMonitor(100);
            def op1 = group.operator(inputs: [a, b], outputs: [c], listeners: [new GracefulShutdownListener(monitor)]) {x, y ->
                sleep 5
                bindOutput x + y
            }
            def op2 = group.operator(inputs: [c], outputs: [d, e], listeners: [new GracefulShutdownListener(monitor)]) {x ->
                sleep 10
                bindAllOutputs 2*x
            }
            def op3 = group.operator(inputs: [d], outputs: [f], listeners: [new GracefulShutdownListener(monitor)]) {x ->
                sleep 5
                bindOutput x + 40
            }
            def op4 = group.operator(inputs: [e.createReadChannel(), f], outputs: [result], listeners: [new GracefulShutdownListener(monitor)]) {x, y ->
                sleep 5
                bindOutput x + y
            }

            100.times{a << 10}
            100.times{b << 20}

            final shutdownPromise = monitor.shutdownNetwork()

            100.times{assert 160 == result.val}

            shutdownPromise.get()
            [op1, op2, op3, op4]*.join()
        }
    }
    public void testGracefulOperatorShutdownWithForks() throws Exception {
        10.times {
            final DataflowQueue a = new DataflowQueue()
            final DataflowQueue b = new DataflowQueue()
            final DataflowQueue c = new DataflowQueue()
            final d = new DataflowQueue<Object>()
            final e = new DataflowBroadcast<Object>()
            final f = new DataflowQueue<Object>()
            final result = new DataflowQueue<Object>()

            final monitor = new GracefulShutdownMonitor(100);
            def op1 = group.operator(inputs: [a, b], outputs: [c], maxForks: 3, listeners: [new GracefulShutdownListener(monitor)]) {x, y ->
                sleep 5
                bindOutput x + y
            }
            def op2 = group.operator(inputs: [c], outputs: [d, e], maxForks: 3, listeners: [new GracefulShutdownListener(monitor)]) {x ->
                sleep 10
                bindAllOutputs 2*x
            }
            def op3 = group.operator(inputs: [d], outputs: [f], maxForks: 4, listeners: [new GracefulShutdownListener(monitor)]) {x ->
                sleep 5
                bindOutput x + 40
            }
            def op4 = group.operator(inputs: [e.createReadChannel(), f], outputs: [result], maxForks: 5, listeners: [new GracefulShutdownListener(monitor)]) {x, y ->
                sleep 5
                bindOutput x + y
            }

            100.times{a << 10}
            100.times{b << 20}

            final shutdownPromise = monitor.shutdownNetwork()

            100.times{assert 160 == result.val}

            shutdownPromise.get()
            [op1, op2, op3, op4]*.join()
        }
    }

    public void testGracefulOperatorShutdownWithSameMessages() throws Exception {
        10.times {
            final DataflowQueue a = new DataflowQueue()
            final DataflowQueue b = new DataflowQueue()
            final DataflowQueue c = new DataflowQueue()
            final d = new DataflowQueue<Object>()
            final e = new DataflowBroadcast<Object>()
            final f = new DataflowQueue<Object>()
            final result = new DataflowQueue<Object>()

            final monitor = new GracefulShutdownMonitor(100);
            def op1 = group.operator(inputs: [a, b], outputs: [c], listeners: [new GracefulShutdownListener(monitor)]) {x, y ->
                sleep 5
                bindOutput x + y
            }
            def op2 = group.operator(inputs: [c], outputs: [d, e], listeners: [new GracefulShutdownListener(monitor)]) {x ->
                sleep 10
                bindAllOutputs 2*x
            }
            def op3 = group.operator(inputs: [d], outputs: [f], listeners: [new GracefulShutdownListener(monitor)]) {x ->
                sleep 5
                bindOutput x + 40
            }
            def op4 = group.operator(inputs: [e.createReadChannel(), f], outputs: [result], listeners: [new GracefulShutdownListener(monitor)]) {x, y ->
                sleep 5
                bindOutput x + y
            }

            100.times{a << 10}
            100.times{b << 10}

            final shutdownPromise = monitor.shutdownNetwork()

            100.times{assert 120 == result.val}

            shutdownPromise.get()
            [op1, op2, op3, op4]*.join()
        }
    }
}
