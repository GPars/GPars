//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.actors

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class AbstractActorTest extends GroovyTestCase {

    public void testActorState() {
        Actor actor=new DefaultTestActor()
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

        while(actor.isActive()) Thread.sleep(100)

        shouldFail(IllegalStateException) {
            actor.send("Message")
        }
        actor.stop()

        shouldFail(IllegalStateException) {
            actor.start()
        }
        actor.stop()
    }

    public void testAfterStart() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        def actor = [act:{->
            Thread.sleep(100)
        },
        afterStart:{->
            flag.set(true)
            latch.countDown()
        }] as DefaultThreadActor

        actor.start()
        latch.await(30, TimeUnit.SECONDS)
        assert flag.get()
        actor.stop()
    }

    public void testMessagingWithTimeout() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final AtomicBoolean receiveFlag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)
        final AtomicReference result = new AtomicReference()

        Actor actor=Actors.actor {
            receive(1, TimeUnit.SECONDS) {
                receiveFlag.set(true)
                result.set it
            }
            flag.set(true)
            latch.countDown()
            stop()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assert flag.get()
        assert receiveFlag.get()
        assertNull result.get()
    }

    public void testInterruption() {
        final InterruptionTestActor actor = new InterruptionTestActor()
        actor.start()
        actor.startLatch.await(30, TimeUnit.SECONDS)
        actor.stop()

        actor.stopLatch.await(30, TimeUnit.SECONDS)
        assert actor.afterStopFlag.get()
        assert !actor.proceedFlag.get()
        assert actor.deliveredMessages.isEmpty()
        assert actor.undeliveredMessages.get().isEmpty()
    }

    public void testUndeliveredMessages() {
        final AfterStopTestActor actor = new AfterStopTestActor()
        actor.start()
        actor.send('Message 1')
        actor.startLatch.await(30, TimeUnit.SECONDS)
        actor.send('Message 2')
        actor.send('Message 3')

        actor.stop()

        actor.stopLatch.await(30, TimeUnit.SECONDS)
        assert actor.afterStopFlag.get()
        assert !actor.proceedFlag.get()
        assert actor.deliveredMessages.contains('Message 1')
        assert actor.undeliveredMessages.get().contains('Message 2')
        assert actor.undeliveredMessages.get().contains('Message 3')
        assertEquals 1, actor.deliveredMessages.size()
        assertEquals 2, actor.undeliveredMessages.get().size()
    }
}

class InterruptionTestActor extends DefaultThreadActor {

    final AtomicBoolean proceedFlag = new AtomicBoolean(false)
    final AtomicBoolean afterStopFlag = new AtomicBoolean(false)
    final CountDownLatch startLatch = new CountDownLatch(1)
    final CountDownLatch stopLatch = new CountDownLatch(1)
    volatile Set deliveredMessages = Collections.synchronizedSet(new HashSet())

    final AtomicReference undeliveredMessages=new AtomicReference()

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

class AfterStopTestActor extends DefaultThreadActor {

    final AtomicBoolean proceedFlag = new AtomicBoolean(false)
    final AtomicBoolean afterStopFlag = new AtomicBoolean(false)
    final CountDownLatch startLatch = new CountDownLatch(1)
    final CountDownLatch stopLatch = new CountDownLatch(1)
    final CountDownLatch receiveLatchLatch = new CountDownLatch(1)
    volatile Set deliveredMessages= Collections.synchronizedSet(new HashSet())

    final AtomicReference undeliveredMessages=new AtomicReference()

    @Override protected void act() {
        String message1 = receive()
        deliveredMessages.add(message1)
        startLatch.countDown()

        receiveLatchLatch.await(30, TimeUnit.SECONDS)  //never opens, throws InterruptedException instead

        proceedFlag.set(true)  //should never reach this line
    }

    public void afterStop(List undeliveredMessages) {
        afterStopFlag.set(true)
        this.undeliveredMessages.set(undeliveredMessages*.payLoad)
        stopLatch.countDown()
    }
}

