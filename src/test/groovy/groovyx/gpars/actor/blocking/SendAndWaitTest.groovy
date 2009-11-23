//  GPars (formerly GParallelizer)
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

package groovyx.gpars.actor.blocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.PooledActorGroup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

public class SendAndWaitTest extends GroovyTestCase {

    public void testSuccessfulMessages() {
        CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.actor {
            delegate.metaClass.afterStop = {
                latch.countDown()
            }

            receive() {
                reply 2
            }
        }

        def result = actor.sendAndWait(1)

        latch.await()
        assertEquals 2, result
    }

    public void testMessagesToStoppedActor() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.actor {
            delegate.metaClass.afterStop = {
                barrier.await()
            }

            receive()
            reply 2
        }

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

        final Actor actor = Actors.actor {
            delegate.metaClass.afterStop = {
                latch.countDown()
            }

            receive()
            reply 2
            barrier.await()
            Thread.sleep 3000  //give the second message time to hit the queue
        }

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

        final Actor actor = Actors.actor {
            delegate.metaClass {
                onException = {}
                afterStop = {
                    latch.countDown()
                }
            }

            receive()
            reply 2
            barrier.await()
            Thread.sleep 3000  //give the second message time to hit the queue
            throw new RuntimeException('test')
        }

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

        final Actor actor = Actors.actor {
            delegate.metaClass.afterStop = {
                latch.countDown()
            }

            barrier.await()
            Thread.sleep 1000
            receive() {
                reply 2
                receive()
                reply 4
            }
        }

        barrier.await()
        def result1 = actor.sendAndWait(1, 5, TimeUnit.SECONDS)
        def result2 = actor.sendAndWait(3, 5, TimeUnit.SECONDS)

        latch.await()
        assertEquals 2, result1
        assertEquals 4, result2
    }

    public void testTimeoutMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.actor {
            delegate.metaClass.afterStop = {
                latch.countDown()
            }

            barrier.await()
            receive()
            barrier.await()
        }

        barrier.await()
        def result = actor.sendAndWait(1, 2, TimeUnit.SECONDS)
        barrier.await()

        latch.await()
        assertNull result
    }

    public void testTimeoutWithActorStopMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.actor {
            delegate.metaClass.afterStop = {
                latch.countDown()
            }

            barrier.await()
            receive()
        }

        barrier.await()
        def result = actor.sendAndWait(1, 2, TimeUnit.SECONDS)

        latch.await()
        assertNull result
    }

    public void testSuccessfulMessagesFromActor() {
        CountDownLatch latch = new CountDownLatch(1)

        final PooledActorGroup group = new PooledActorGroup(3)

        final Actor actor = group.actor {
            receive {
                reply 2
            }
        }

        volatile def result

        group.actor {
            result = actor.sendAndWait(1)
            latch.countDown()
        }

        latch.await()
        assertEquals 2, result
    }
}
