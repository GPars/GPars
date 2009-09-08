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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

public class SendAndWaitTest extends GroovyTestCase {

    public void testSuccessfulMessages() {
        CountDownLatch latch = new CountDownLatch(1)

        final DefaultThreadActor actor = Actors.oneShotActor {
            receive() {
                reply 2
            }
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def result = actor.sendAndWait(1)

        latch.await()
        assertEquals 2, result
    }

    public void testMessagesToStoppedActor() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DefaultThreadActor actor = Actors.oneShotActor {
            receive()
            reply 2
        }

        actor.metaClass.afterStop = {
            barrier.await()
        }

        actor.start()

        def result = actor.sendAndWait(1)
        barrier.await()
        shouldFail(IllegalStateException) {
            actor.sendAndWait 2
        }

        assertEquals 2, result
    }

    public void testFailedMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DefaultThreadActor actor = Actors.oneShotActor {
            receive()
            reply 2
            barrier.await()
            Thread.sleep 3000  //give the second message time to hit the queue
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def result = actor.sendAndWait(1)
        barrier.await()
        shouldFail(IllegalStateException) {
            actor.sendAndWait 2
        }

        latch.await()
        assertEquals 2, result
    }

    public void testFailedMessagesOnException() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DefaultThreadActor actor = Actors.oneShotActor {
            receive()
            reply 2
            barrier.await()
            Thread.sleep 3000  //give the second message time to hit the queue
            if (true) throw new RuntimeException('test')
        }

        actor.metaClass {
            onException = {}
            afterStop = {
                latch.countDown()
            }
        }

        actor.start()

        def result = actor.sendAndWait(1)
        barrier.await()
        shouldFail(IllegalStateException) {
            actor.sendAndWait 2
        }

        latch.await()
        assertEquals 2, result
    }

    public void testTimeoutSuccessfulMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DefaultThreadActor actor = Actors.oneShotActor {
            barrier.await()
            Thread.sleep 1000
            receive() {
                reply 2
                receive()
                reply 4
            }
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        barrier.await()
        def result1 = actor.sendAndWait(5, TimeUnit.SECONDS, 1)
        def result2 = actor.sendAndWait(5, TimeUnit.SECONDS, 3)

        latch.await()
        assertEquals 2, result1
        assertEquals 4, result2
    }

    public void testTimeoutMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DefaultThreadActor actor = Actors.oneShotActor {
            barrier.await()
            receive()
            barrier.await()
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        barrier.await()
        def result = actor.sendAndWait(2, TimeUnit.SECONDS, 1)
        barrier.await()

        latch.await()
        assertNull result
    }

    public void testTimeoutWithActorStopMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DefaultThreadActor actor = Actors.oneShotActor {
            barrier.await()
            receive()
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        barrier.await()
        def result = actor.sendAndWait(2, TimeUnit.SECONDS, 1)

        latch.await()
        assertNull result
    }

    public void testSuccessfulMessagesFromActor() {
        CountDownLatch latch = new CountDownLatch(1)

        final AbstractThreadActorGroup group = new ThreadActorGroup(3, true)

        final Actor actor = group.oneShotActor {
            receive {
                reply 2
            }
        }
        actor.start()

        volatile def result

        group.oneShotActor {
            result = actor.sendAndWait(1)
            latch.countDown()
        }.start()

        latch.await()
        assertEquals 2, result
    }
}
