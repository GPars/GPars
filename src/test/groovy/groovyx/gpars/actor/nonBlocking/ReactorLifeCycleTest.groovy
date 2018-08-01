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

package groovyx.gpars.actor.nonBlocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 *
 * @author Vaclav Pech
 * Date: Aug 24, 2010
 */
public class ReactorLifeCycleTest extends GroovyTestCase {

    PGroup group

    protected void setUp() {
        group = new DefaultPGroup(5)
    }

    protected void tearDown() {
        group.shutdown()
    }

    public void testInternalStop() {
        final barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)
        def unprocessedMessages = []

        final Actor actor = group.reactor {
            barrier.await()
            counter.incrementAndGet()
            stop()
        }

        actor.metaClass {
            afterStop = {List messages ->
                unprocessedMessages = messages
            }
        }

        actor << 1
        assert actor.isActive()
        actor << 2
        actor << 3
        barrier.await()
        barrier.await()
        barrier.await()
        actor.join()
        assert !actor.isActive()
        assert 3 == counter.get()
        assert [] == unprocessedMessages
    }

    public void testExternalStop() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.reactor {
            barrier.await()
            counter.incrementAndGet()
        }

        actor << 1
        assert actor.isActive()
        assert 0 == counter.intValue()
        actor.stop()
        barrier.await()
        actor.join()
        assert 1 == counter.intValue()
        assertFalse actor.isActive()
        assert 1 == counter.get()

        shouldFail(IllegalStateException) {
            actor.send 'message'
        }
    }

    public void testInternalTerminate() {
        final barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)
        def unprocessedMessages = []

        final Actor actor = group.reactor {
            barrier.await()
            counter.incrementAndGet()
            barrier.await()
            terminate()
        }

        actor.metaClass {
            afterStop = {List messages ->
                unprocessedMessages = messages
            }
        }

        actor << 1
        barrier.await()
        assert actor.isActive()
        actor << 2
        actor << 3
        barrier.await()
        assert 1 == counter.intValue()
        actor.join()
        assertFalse actor.isActive()
        assert [2, 3] == unprocessedMessages.collect {it.payLoad}
    }

    public void testExternalTerminate() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.reactor {
            barrier.await()
            counter.incrementAndGet()
        }

        actor << 1
        barrier.await()
        assert actor.isActive()
        actor.terminate()
        actor.join()
        assert 1 == counter.intValue()
        assertFalse actor.isActive()

        shouldFail(IllegalStateException) {
            actor.send 'message'
        }
    }

    public void testTerminate() {
        final AtomicInteger counter = new AtomicInteger(0)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final def group = new NonDaemonPGroup(1)
        final Actor actor = group.reactor {
            barrier.await()
            Thread.sleep 30000
            if (Thread.currentThread().isInterrupted()) return 0
            counter.set 10
            10
        }

        actor.send('message')
        barrier.await()
        actor.terminate()
        actor.join()

        assert 0 == counter.intValue()

        shouldFail(IllegalStateException) {
            actor.send 'message'
        }
        group.shutdown()
    }

    public void testReentrantStop() {
        final def latch = new CountDownLatch(1)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.reactor {
            10
        }

        actor.metaClass {
            afterStop = {List messages ->
                counter.incrementAndGet()
                latch.countDown()
            }
            onInterrupt = {}
        }

        actor.send 'message'
        actor.stop()
        actor.stop()
        latch.await()
        assert 1 == counter.intValue()
        assert actor.hasBeenStopped()
    }

    public void testStopWithoutMessageSent() {
        final def latch = new CountDownLatch(1)
        final AtomicInteger counter = new AtomicInteger(0)
        AtomicReference messagesReference = new AtomicReference(null)

        final Actor actor = group.reactor {
            counter.incrementAndGet()
        }

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                latch.countDown()
            }
        }

        assert actor.isActive()
        assert 0 == counter.intValue()

        actor.stop().join()

        latch.await(90, TimeUnit.SECONDS)
        assertFalse actor.isActive()
        assert 0 == counter.intValue()
        assertNotNull messagesReference.get()
    }

    public void testTerminateWithoutMessageSent() {
        final def latch = new CountDownLatch(1)
        final AtomicInteger counter = new AtomicInteger(0)
        AtomicReference messagesReference = new AtomicReference(null)

        final Actor actor = group.reactor {
            counter.incrementAndGet()
        }

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                latch.countDown()
            }
        }

        assert actor.isActive()
        assert 0 == counter.intValue()

        actor.terminate().join()

        latch.await(90, TimeUnit.SECONDS)
        assertFalse actor.isActive()
        assert 0 == counter.intValue()
        assertNotNull messagesReference.get()
    }

    public void testStopWithInterruption() {
        final def barrier = new CyclicBarrier(2)
        final def latch = new CountDownLatch(1)
        final AtomicInteger counter = new AtomicInteger(0)
        AtomicReference<List> messagesReference = new AtomicReference<List>(null)

        final Actor actor = group.reactor {
            barrier.await()
            Thread.sleep(30000)
            counter.incrementAndGet()  //never reached
        }

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                latch.countDown()
            }
            onInterrupt = {}
        }

        actor.send 'message1'
        actor.send 'message2'
        actor.send 'message3'
        barrier.await()
        assert actor.isActive()
        Thread.sleep 500
        actor.terminate().join()

        latch.await(90, TimeUnit.SECONDS)
        assert 0 == counter.intValue()
        assertFalse actor.isActive()
        assertNotNull messagesReference.get()
        assert 2 == messagesReference.get().size()
    }

    public void testOnInterrupt() {
        final def barrier = new CyclicBarrier(2)
        final def afterStopBarrier = new CyclicBarrier(2)
        final AtomicBoolean onInterruptFlag = new AtomicBoolean(false)
        AtomicReference<List> messagesReference = new AtomicReference<List>(null)

        final Actor actor = group.fairReactor {
            barrier.await()
            Thread.sleep(30000)
        }

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                afterStopBarrier.await()
            }
            onInterrupt = { onInterruptFlag.set true }
        }

        actor.send 'message1'
        actor.send 'message2'
        actor.send 'message3'
        barrier.await()
        actor.terminate()

        afterStopBarrier.await(90, TimeUnit.SECONDS)
        assert onInterruptFlag.get()
        assertFalse actor.isActive()
        assertNotNull messagesReference.get()
        assert 2 == messagesReference.get().size()
    }

    public void testExplicitInterruption() {
        final def barrier = new CyclicBarrier(2)
        final def afterStopBarrier = new CyclicBarrier(2)
        final AtomicBoolean onInterruptFlag = new AtomicBoolean(false)
        AtomicReference<List> messagesReference = new AtomicReference<List>(null)

        final Actor actor = group.reactor {
            barrier.await()
            Thread.currentThread().interrupt()
        }

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                afterStopBarrier.await()
            }
            onInterrupt = { onInterruptFlag.set true }
        }

        actor.send 'message1'
        actor.send 'message2'
        actor.send 'message3'
        barrier.await()

        afterStopBarrier.await(90, TimeUnit.SECONDS)
        assert onInterruptFlag.get()
        assertFalse actor.isActive()
        assertNotNull messagesReference.get()
        assert 2 == messagesReference.get().size()
    }

    public void testOnException() {
        final def barrier = new CyclicBarrier(2)
        final def afterStopBarrier = new CyclicBarrier(2)
        final AtomicBoolean onExceptionFlag = new AtomicBoolean(false)
        AtomicReference<List> messagesReference = new AtomicReference<List>(null)

        final Actor actor = group.reactor {
            barrier.await()
            throw new RuntimeException('test')
        }

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                afterStopBarrier.await()
            }
            onException = { onExceptionFlag.set true }
        }

        actor.send 'message1'
        actor.send 'message2'
        actor.send 'message3'
        barrier.await()

        afterStopBarrier.await(90, TimeUnit.SECONDS)
        assert onExceptionFlag.get()
        assertFalse actor.isActive()
        assertNotNull messagesReference.get()
        assert 2 == messagesReference.get().size()
    }

    public void testRestart() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.reactor {
            barrier.await()
            counter.incrementAndGet()
            barrier.await()
            stop()
        }

        actor << 1
        barrier.await()
        assert actor.isActive()
        barrier.await()
        assert 1 == counter.intValue()
        actor.join()
        assertFalse actor.isActive()

        actor.start()
    }

    public void testDoubleStart() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.fairReactor {
            barrier.await()
        }

        shouldFail(IllegalStateException) {
            actor.start()
        }

        actor << 1
        assert actor.isActive()
        barrier.await()
        actor.stop().join()
        assertFalse actor.isActive()
    }
}
