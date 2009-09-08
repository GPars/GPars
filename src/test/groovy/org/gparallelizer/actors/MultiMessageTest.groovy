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
import java.util.concurrent.TimeUnit
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger

public class MultiMessageTest extends GroovyTestCase {
    public void testReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            receive {a, b, c ->
                result = a + b + c
                latch.countDown()
            }
        }.start()

        actor.send 2
        actor.send 3
        actor.send 4

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 9, result
    }

    public void testNoMessageReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            receive {->
                result = 1
                latch.countDown()
            }
        }.start()

        actor.send 2

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 1, result
    }

    public void testDefaultMessageReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            receive {
                result = 1
                latch.countDown()
            }
        }.start()

        actor.send 2

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 1, result
    }

    public void testArrayReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            receive {a, b, c ->
                result = a[2] + b + c
                latch.countDown()
            }
        }.start()

        actor.send([2, 10, 20])
        actor.send 3
        actor.send 4

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 27, result
    }

    public void testMessageReply() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(3)
        volatile AtomicInteger result = new AtomicInteger(0)

        def actor = Actors.oneShotActor {
            receive {a, b, c ->
                a.reply(a + 1)
                b.reply(b + 1)
                c.reply(c + 1)
            }
        }.start()

        createReplyActor actor, 10, barrier, latch, result
        createReplyActor actor, 100, barrier, latch, result
        createReplyActor actor, 1000, barrier, latch, result

        latch.await(30, TimeUnit.SECONDS)

        assertEquals 1113, result.get()
    }

   public void testActorReply() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(3)
        volatile AtomicInteger result = new AtomicInteger(0)

        def actor = Actors.oneShotActor {
            receive {a, b, c ->
                reply(20)
            }
        }.start()

        createReplyActor actor, 10, barrier, latch, result
        createReplyActor actor, 100, barrier, latch, result
        createReplyActor actor, 1000, barrier, latch, result

        latch.await(30, TimeUnit.SECONDS)

        assertEquals 60, result.get()
    }

    Actor createReplyActor(Actor actor, int num, CyclicBarrier barrier, CountDownLatch latch, AtomicInteger result) {
        Actors.oneShotActor {
            barrier.await()
            actor.send(num)
            receive {
                result.addAndGet(it)
                latch.countDown()
            }
        }.start()
    }    
}
