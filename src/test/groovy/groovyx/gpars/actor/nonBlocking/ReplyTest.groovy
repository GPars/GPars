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

package groovyx.gpars.actor.nonBlocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.impl.AbstractPooledActor
import groovyx.gpars.actor.impl.ActorReplyException
import groovyx.gpars.dataflow.DataFlowVariable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean
import static groovyx.gpars.actor.Actors.actor

/**
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public class ReplyTest extends GroovyTestCase {

    protected void setUp() {
        super.setUp();
        Actors.defaultPooledActorGroup.resize(5)
    }

    public void testMultipleClients() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def bouncer = actor {
            loop {
                react {
                    reply it
                    barrier.await()
                }
            }
        }

        Thread.sleep 1000

        actor {
            bouncer.send 1
            barrier.await()
            bouncer.send 2
            barrier.await()
            barrier.await()
            barrier.await()
            react {
                replies1 << it
                react {
                    replies1 << it
                    completedBarrier.await()
                }
            }
        }

        actor {
            bouncer.send 10
            barrier.await()
            bouncer.send 20
            barrier.await()
            barrier.await()
            barrier.await()
            react {
                replies2 << it
                react {
                    replies2 << it
                    completedBarrier.await()
                }
            }
        }

        completedBarrier.await()
        bouncer.stop()

        assertEquals([1, 2], replies1)
        assertEquals([10, 20], replies2)
    }

    public void testMultipleActors() {
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def incrementor = actor {
            loop {
                react { reply it + 1 }
            }
        }

        final def decrementor = actor {
            loop {
                react { reply it - 1 }
            }
        }

        actor {
            barrier.await()
            incrementor.send 2
            decrementor.send 6
            incrementor.send 3
            decrementor.send 9
            react {
                replies1 << it
                react {
                    replies1 << it
                    react {
                        replies1 << it
                        react {
                            replies1 << it
                            completedBarrier.await()
                        }
                    }
                }
            }
        }

        actor {
            barrier.await()
            incrementor.send 20
            decrementor.send 60
            incrementor.send 30
            decrementor.send 40
            react {
                replies2 << it
                react {
                    replies2 << it
                    react {
                        replies2 << it
                        react {
                            replies2 << it
                            completedBarrier.await()
                        }
                    }
                }
            }
        }

        completedBarrier.await()
        incrementor.stop()
        decrementor.stop()
        assertEquals 4, replies1.size()
        assert replies1.containsAll([3, 5, 4, 8])
        assertEquals 4, replies2.size()
        assert replies2.containsAll([21, 59, 31, 39])
    }

    public void testReplyWithoutSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final AbstractPooledActor actor = actor {
            delegate.metaClass {
                onException = {
                    flag.set(true)
                    barrier.await()
                }
            }

            react {
                reply it
            }
        }

        actor.send 'messsage'
        barrier.await()

        assert flag.get()
    }

    public void testReplyIfExists() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final AbstractPooledActor receiver = actor {
            react {
                replyIfExists it
            }
        }

        actor {
            receiver.send 'messsage'
            react {
                flag.set(true)
                barrier.await()
            }
        }

        barrier.await()

        assert flag.get()
    }

    public void testReplyIfExistsWithoutSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final AbstractPooledActor actor = actor {
            react {
                replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        actor.send 'messsage'
        barrier.await()

        assert flag.get()
    }

    public void testReplyIfExistsWithStoppedSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final CountDownLatch latch = new CountDownLatch(1)

        final AbstractPooledActor replier = actor {
            react {
                latch.await()
                replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        final AbstractPooledActor sender = actor {
            delegate.metaClass {
                afterStop = {
                    latch.countDown()
                }
            }

            replier.send 'messsage'
        }

        latch.await()
        barrier.await()

        assert flag.get()
    }

    public void testReplyToReply() {
        volatile String result

        final CountDownLatch latch = new CountDownLatch(1)

        final AbstractPooledActor actor = Actors.actor {
            react {
                reply 'Message2'
                it.reply 'Message3'
                react {a, b ->
                    reply 'Message6'
                    latch.await()
                }
            }
        }

        Actors.actor {
            actor.send 'Message1'
            react {
                it.reply 'Message4'
                react {
                    reply 'Message5'
                    react {a, b ->
                        result = a + b
                        latch.countDown()
                    }
                }
            }

        }

        latch.await()
        assertEquals 'Message6Message6', result
    }

    public void testReplyFromNoArgHandler() {
        volatile String result

        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = actor {
            react {->
                reply 'Message2'
            }
        }

        Actors.actor {
            actor.send 'Message1'
            react {
                result = it
                latch.countDown()
            }

        }

        latch.await()
        assertEquals 'Message2', result

    }

    public void testMultipleClientsWithReply() {
        final List<CountDownLatch> latches = [new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(2)]
        def volatile issues

        final def bouncer = actor {
            latches[0].await()
            react {a, b, c ->
                replyIfExists 4
                latches[1].countDown()

                latches[2].await()
                react {x, y, z ->
                    try {
                        reply 8
                    } catch (ActorReplyException e) {
                        issues = e.issues
                        latches[3].countDown()
                    }
                }
            }
        }

        //send and terminate
        final AbstractPooledActor actor1 = actor {
            delegate.metaClass.afterStop = {
                latches[0].countDown()
            }

            bouncer << 1
        }

        //wait, send and terminate
        final AbstractPooledActor actor2 = actor {
            delegate.metaClass.afterStop = {
                latches[2].countDown()
            }

            latches[1].await()
            bouncer << 5
        }

        //keep conversation going
        actor {
            bouncer << 2
            react {
                bouncer << 6
                react {
                    latches[3].countDown()
                }
            }
        }

        bouncer << 3
        latches[2].await()
        bouncer << 7
        latches[3].await()
        assertEquals 2, issues.size()
        assert (issues[0] instanceof IllegalArgumentException) || (issues[1] instanceof IllegalArgumentException)
        assert (issues[0] instanceof IllegalStateException) || (issues[1] instanceof IllegalStateException)
    }

    public void testOriginatorDetection() {
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        final DataFlowVariable originator1 = new DataFlowVariable()
        final DataFlowVariable originator2 = new DataFlowVariable()
        final DataFlowVariable originator3 = new DataFlowVariable()

        final def bouncer = actor {
            react {msg1 ->
                originator1 << msg1.sender
                react {msg2 ->
                    originator2 << msg2.sender
                    react {msg3 ->
                        originator3 << msg3.sender
                    }
                }
            }
        }

        final def actor1 = actor {
            bouncer << 'msg1'
        }

        assert actor1 == originator1.val

        final def actor2 = actor {
            bouncer << 'msg2'
        }

        assert actor2 == originator2.val

        bouncer << 'msg3'

        assertNull originator3.val
    }
}
