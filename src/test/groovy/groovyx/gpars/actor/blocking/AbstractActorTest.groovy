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

package groovyx.gpars.actor.blocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.BlockingActor
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.group.DefaultPGroup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Vaclav Pech, Dierk Koenig
 * Date: Jan 7, 2009
 */
public class AbstractActorTest extends GroovyTestCase {

    public void testActorState() {
        Actor actor = new DefaultTestActor()
        shouldFail(IllegalStateException) {
            actor.send("Message")
        }
        actor.stop()
        actor.start()
        shouldFail(IllegalStateException) {
            actor.start()
        }

        actor.stop()
        actor.stop()
        actor.stop()

        while (actor.isActive()) Thread.sleep(100)

        shouldFail(IllegalStateException) {
            actor.send("Message")
        }
        actor.stop()

        shouldFail(IllegalStateException) {
            actor.start()
        }
        actor.stop()
    }

    public void testMessageMayBeNull() {
        Actor actor = new DefaultTestActor()
        actor.start()
        actor.send null
        assert actor.receiveWasCalled
    }

    public void testAfterStart() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        def actor = new BlockingActor() {
            @Override
            protected void act() {
                Thread.sleep(100)
            }
            public void afterStart() {
                flag.set(true)
                latch.countDown()
            }
        }

        actor.start()
        latch.await(90, TimeUnit.SECONDS)
        assert flag.get()
        actor.stop()
    }

    public void testMessagingWithTimeout() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final AtomicBoolean receiveFlag = new AtomicBoolean(false)
        final AtomicBoolean timeoutFlag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)
        final AtomicReference result = new AtomicReference()

        Actor actor = Actors.blockingActor {
            delegate.metaClass.onTimeout = {-> timeoutFlag.set(true)}

            receive(1, TimeUnit.SECONDS) {
                receiveFlag.set(true)
                result.set it
            }
            flag.set(true)
            latch.countDown()
        }

        latch.await(90, TimeUnit.SECONDS)
        assert flag.get()
        assert receiveFlag.get()
        assert timeoutFlag.get()
        assert Actor.TIMEOUT == result.get()
    }

    public void testInterruption() {
        final InterruptionTestActor actor = new InterruptionTestActor()
        actor.start()
        actor.startLatch.await(90, TimeUnit.SECONDS)
        actor.terminate()

        actor.stopLatch.await(90, TimeUnit.SECONDS)
        assert actor.afterStopFlag.get()
        assert !actor.proceedFlag.get()
        assert actor.deliveredMessages.isEmpty()
        assert actor.undeliveredMessages.get().isEmpty()
    }

    public void testUndeliveredMessages() {
        final AfterStopTestActor actor = new AfterStopTestActor()
        actor.start()
        actor.send('Message 1')
        actor.startLatch.await(90, TimeUnit.SECONDS)
        actor.send('Message 2')
        actor.send('Message 3')

        actor.terminate()

        actor.stopLatch.await(90, TimeUnit.SECONDS)
        assert actor.afterStopFlag.get()
        assert !actor.proceedFlag.get()
        assert actor.deliveredMessages.contains('Message 1')
        assert actor.undeliveredMessages.get().contains('Message 2')
        assert actor.undeliveredMessages.get().contains('Message 3')
        assert 1 == actor.deliveredMessages.size()
        assert 2 == actor.undeliveredMessages.get().size()
    }

    public void testSendAndPromise() {
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final DataflowVariable result = new DataflowVariable()

        final DefaultPGroup group = new DefaultPGroup()
        def a = group.actor {
            barrier.await()
            react {
                reply it * 2
            }
        }
        final Promise<Object> promise = a.sendAndPromise(10)
        assert !promise.isBound()
        promise >> {result << it}
        barrier.await(90, TimeUnit.SECONDS)
        assert promise.get() == 20
        assert promise.isBound()
        assert result.get() == 20
    }
}

class InterruptionTestActor extends BlockingActor {

    final AtomicBoolean proceedFlag = new AtomicBoolean(false)
    final AtomicBoolean afterStopFlag = new AtomicBoolean(false)
    final CountDownLatch startLatch = new CountDownLatch(1)
    final CountDownLatch stopLatch = new CountDownLatch(1)
    volatile Set deliveredMessages = Collections.synchronizedSet(new HashSet())

    final AtomicReference undeliveredMessages = new AtomicReference()

    @Override protected void act() {
        startLatch.countDown()
        receive()
        proceedFlag.set(true)  //should never reach this line
    }

    public void afterStop(List undeliveredMessages) {
        afterStopFlag.set(true)
        this.undeliveredMessages.set(undeliveredMessages)
        stopLatch.countDown()
    }
}

class AfterStopTestActor extends BlockingActor {

    final AtomicBoolean proceedFlag = new AtomicBoolean(false)
    final AtomicBoolean afterStopFlag = new AtomicBoolean(false)
    final CountDownLatch startLatch = new CountDownLatch(1)
    final CountDownLatch stopLatch = new CountDownLatch(1)
    final CountDownLatch receiveLatchLatch = new CountDownLatch(1)
    volatile Set deliveredMessages = Collections.synchronizedSet(new HashSet())

    final AtomicReference undeliveredMessages = new AtomicReference()

    @Override protected void act() {
        String message1 = receive()
        deliveredMessages.add(message1)
        startLatch.countDown()

        receiveLatchLatch.await(90, TimeUnit.SECONDS)  //never opens, throws InterruptedException instead

        proceedFlag.set(true)  //should never reach this line
    }

    public void afterStop(List undeliveredMessages) {
        afterStopFlag.set(true)
        this.undeliveredMessages.set(undeliveredMessages*.payLoad)
        stopLatch.countDown()
    }
}