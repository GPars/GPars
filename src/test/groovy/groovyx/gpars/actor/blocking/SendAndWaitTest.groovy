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
import groovyx.gpars.group.DefaultPGroup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

abstract public class SendAndWaitTest extends GroovyTestCase {

    public void testSuccessfulMessages() {
        CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.blockingActor {
            delegate.metaClass.afterStop = {
                latch.countDown()
            }

            receive() {
                reply 2
            }
        }

        def result = actor.sendAndWait(1)

        latch.await()
        assert 2 == result
    }

    public void testMessagesToStoppedActor() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.blockingActor {
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

        assert 2 == result
    }

    public void testFailedMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.blockingActor {
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
        assert 2 == result
    }

    public void testFailedMessagesOnException() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.blockingActor {
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
        assert 2 == result
    }

    public void testTimeoutSuccessfulMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.blockingActor {
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
        def result1 = actor.sendAndWait(1, 30, TimeUnit.SECONDS)
        def result2 = actor.sendAndWait(3, 30, TimeUnit.SECONDS)

        latch.await()
        assert 2 == result1
        assert 4 == result2
    }

    public void testTimeoutMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.blockingActor {
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

        final Actor actor = Actors.blockingActor {
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

        final DefaultPGroup group = new DefaultPGroup(3)

        final Actor actor = group.blockingActor {
            receive {
                reply 2
            }
        }

        def result

        group.actor {
            result = actor.sendAndWait(1)
            latch.countDown()
        }

        latch.await()
        assert 2 == result
        group.shutdown()
    }
}
