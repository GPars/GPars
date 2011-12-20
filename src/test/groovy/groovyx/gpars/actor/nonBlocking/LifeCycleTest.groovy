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
import groovyx.gpars.dataflow.Dataflows
import groovyx.gpars.group.DefaultPGroup
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
 * Date: Feb 19, 2009
 */
public class LifeCycleTest extends GroovyTestCase {

    PGroup group

    protected void setUp() {
        group = new DefaultPGroup(5)
    }

    protected void tearDown() {
        group.shutdown()
    }


    public void testDefaultStop() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.actor {
            barrier.await()
            counter.incrementAndGet()
            barrier.await()
        }

        barrier.await()
        assert actor.isActive()
        barrier.await()
        assert 1 == counter.intValue()
        actor.join()
        assertFalse actor.isActive()
    }

    public void testDefaultStopAfterReact() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)
        AtomicReference messagesReference = new AtomicReference(null)

        final Actor actor = group.actor {
            react {
                barrier.await()
                counter.incrementAndGet()
                barrier.await()
            }
        }

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                barrier.await()
            }
        }

        actor.send 'message'
        barrier.await()
        assert actor.isActive()
        barrier.await()
        assert 1 == counter.intValue()
        barrier.await()
        assertFalse actor.isActive()
        assertNotNull messagesReference.get()
    }

    public void testStop() {
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.actor {
            Thread.sleep 2000
            react {
                counter.incrementAndGet()
            }
        }

        actor.metaClass {
            onInterrupt = {}
        }

        actor.send('message')
        actor.stop().join()

        assert 1 == counter.intValue()

        shouldFail(IllegalStateException) {
            actor.send 'message'
        }
    }

    public void testTerminate() {
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.actor {
            Thread.sleep 10000
            react {
                counter.incrementAndGet()
            }
        }

        actor.metaClass {
            onInterrupt = {}
        }

        actor.send('message')
        actor.terminate().join()

        assert 0 == counter.intValue()

        shouldFail(IllegalStateException) {
            actor.send 'message'
        }
    }

    public void testReentrantStop() {
        final def latch = new CountDownLatch(1)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.actor {
            react {
            }
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
        assertFalse actor.isActive()
    }

    public void testStopWithoutMessageSent() {
        final def barrier = new CyclicBarrier(2)
        final def latch = new CountDownLatch(1)
        final AtomicInteger counter = new AtomicInteger(0)
        AtomicReference messagesReference = new AtomicReference(null)

        final Actor actor = group.actor {
            counter.incrementAndGet()
            barrier.await()

            react {
                counter.incrementAndGet()
            }
        }

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                latch.countDown()
            }
        }

        barrier.await()
        assert actor.isActive()
        assert 1 == counter.intValue()
        Thread.sleep 500

        actor.terminate().join()

        latch.await(90, TimeUnit.SECONDS)
        assertFalse actor.isActive()
        assert 1 == counter.intValue()
        assertNotNull messagesReference.get()
    }

    public void testStopWithInterruption() {
        final def barrier = new CyclicBarrier(2)
        final def latch = new CountDownLatch(1)
        final AtomicInteger counter = new AtomicInteger(0)
        AtomicReference<List> messagesReference = new AtomicReference<List>(null)

        final Actor actor = group.actor {
            react {
                barrier.await()
                Thread.sleep(10000)
                counter.incrementAndGet()  //never reached
            }
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

    public void testStartedByFactory() {
        final def df = new Dataflows()
        def a1 = group.actor {df.actor = true}
        def a2 = group.reactor {df.reactor = true} << ''
        def a3 = group.messageHandler {df.handler = true}


        assert df.actor
        assert df.reactor
        assert df.handler

        a2.terminate()
        a3.terminate()
        [a1, a2, a3]*.join()
    }

    public void testAfterStart() {
        final def afterStartBarrier = new CyclicBarrier(2)
        final AtomicBoolean afterStartFlag = new AtomicBoolean(false)

        group.actor {
            afterStartFlag.set true
            afterStartBarrier.await()
        }

        afterStartBarrier.await(90, TimeUnit.SECONDS)
        assert afterStartFlag.get()
    }

    public void testOnInterrupt() {
        final def barrier = new CyclicBarrier(2)
        final def afterStopBarrier = new CyclicBarrier(2)
        final AtomicBoolean onInterruptFlag = new AtomicBoolean(false)
        AtomicReference<List> messagesReference = new AtomicReference<List>(null)

        final Actor actor = group.actor {
            react {
                barrier.await()
                Thread.sleep(10000)
            }
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

    public void testOnException() {
        final def barrier = new CyclicBarrier(2)
        final def afterStopBarrier = new CyclicBarrier(2)
        final AtomicBoolean onExceptionFlag = new AtomicBoolean(false)
        AtomicReference<List> messagesReference = new AtomicReference<List>(null)

        final Actor actor = group.actor {
            react {
                barrier.await()
                throw new RuntimeException('test')
            }
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

        final Actor actor = group.actor {
            barrier.await()
            counter.incrementAndGet()
            barrier.await()
        }

        barrier.await()
        assert actor.isActive()
        barrier.await()
        assert 1 == counter.intValue()
        actor.join()
        assertFalse actor.isActive()

        shouldFail(IllegalStateException) {
            actor.start()
        }
    }

    public void testDoubleStart() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = group.actor {
            barrier.await()
        }

        shouldFail(IllegalStateException) {
            actor.start()
        }

        assert actor.isActive()
        barrier.await()
        actor.join()
        assertFalse actor.isActive()
    }
}
