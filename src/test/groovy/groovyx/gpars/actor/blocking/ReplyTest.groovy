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
import groovyx.gpars.actor.impl.ActorReplyException
import groovyx.gpars.dataflow.DataFlowVariable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean
import static groovyx.gpars.actor.Actors.actor

/**
 *
 * @author Vaclav Pech
 * Date: Apr 16, 2009
 */
public class ReplyTest extends GroovyTestCase {

    protected void setUp() {
        super.setUp();
    }

    public void testMultipleClients() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def bouncer = Actors.actor {
            loop {
                receive {
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
            receive {
                replies1 << it
                receive {
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
            receive {
                replies2 << it
                receive {
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
        final def group = new PooledActorGroup(5)
        def replies1 = []
        def replies2 = []

        final def incrementor = group.actor {
            loop { receive { reply it + 1 }}
        }

        final def decrementor = group.actor {
            loop { receive { reply it - 1 }}
        }

        group.actor {
            barrier.await()
            incrementor.send 2
            decrementor.send 6
            incrementor.send 3
            decrementor.send 9
            receive {
                replies1 << it
                receive {
                    replies1 << it
                    receive {
                        replies1 << it
                        receive {
                            replies1 << it
                            completedBarrier.await()
                        }
                    }
                }
            }
        }

        group.actor {
            barrier.await()
            incrementor.send 20
            decrementor.send 60
            incrementor.send 30
            decrementor.send 40
            receive {
                replies2 << it
                receive {
                    replies2 << it
                    receive {
                        replies2 << it
                        receive {
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

        final Actor actor = Actors.actor {
            delegate.metaClass {
                onException = {
                    flag.set(true)
                    barrier.await()
                }
            }

            loop {
                receive {
                    reply it
                }
            }
        }

        actor.send 'messsage'
        barrier.await()

        actor.stop()

        assert flag.get()
    }

    public void testReplyIfExists() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor receiver = actor {
            receive {
                replyIfExists it
            }
        }

        actor {
            receiver.send 'messsage'
            receive {
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

        final Actor actor = actor {
            receive {
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

        final Actor replier = actor {
            receive {
                latch.await()
                replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        final Actor sender = actor {
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

        final Actor actor = Actors.actor {
            receive {
                reply 'Message2'
                it.reply 'Message3'
                receive {a, b ->
                    reply 'Message6'
                    latch.await()
                }
            }
        }

        Actors.actor {
            actor.send 'Message1'
            receive {
                it.reply 'Message4'
                receive {
                    reply 'Message5'
                    receive {a, b ->
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

        final Actor actor = Actors.actor {
            receive {->
                reply 'Message2'
            }
        }

        Actors.actor {
            actor.send 'Message1'
            receive {
                result = it
                latch.countDown()
            }

        }

        latch.await()
        assertEquals 'Message2', result

    }

    public void testMultipleClientsWithReply() {
        final List<CountDownLatch> latches = [new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)]
        def volatile issues

        final def bouncer = actor {
            latches[0].await()
            receive {a, b, c ->
                replyIfExists 4
                latches[1].countDown()
            }
            latches[2].await()
            receive {a, b, c ->
                try {
                    reply 8
                } catch (ActorReplyException e) {
                    issues = e.issues
                    latches[3].countDown()
                }
            }
        }

        //send and terminate
        final Actor actor1 = actor {
            delegate.metaClass.afterStop = {
                latches[0].countDown()
            }

            bouncer << 1
            stop()
        }

        //wait, send and terminate
        final Actor actor2 = actor {
            delegate.metaClass.afterStop = {
                latches[2].countDown()
            }

            latches[1].await()
            bouncer << 5
            stop()
        }

        //keep conversation going
        actor {
            bouncer << 2
            receive()
            bouncer << 6
            receive()
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
        final DataFlowVariable originator4 = new DataFlowVariable()

        final def bouncer = actor {
            receive {msg1 ->
                originator1 << msg1.sender
                receive {msg2 ->
                    originator2 << msg2.sender
                    receive {msg3 ->
                        originator3 << msg3.sender
                    }
                    def msg4 = receive()
                    originator4 << msg4.sender
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

        final def actor4 = actor {
            bouncer << 'msg4'
        }

        assert actor4 == originator4.val
    }
}
