// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
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
import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataflowVariable

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

        assert [1, 2] == replies1
        assert [10, 20] == replies2
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
        assert 4 == replies1.size()
        assert replies1.containsAll([3, 5, 4, 8])
        assert 4 == replies2.size()
        assert replies2.containsAll([21, 59, 31, 39])
    }

    public void testReplyWithoutSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = actor {
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

        actor.send 'message'
        barrier.await()

        assert flag.get()
    }

    public void testReplyIfExists() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor receiver = actor {
            react {
                replyIfExists it
            }
        }

        actor {
            receiver.send 'message'
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

        final Actor actor = actor {
            react {
                replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        actor.send 'message'
        barrier.await()

        assert flag.get()
    }

    public void testReplyIfExistsWithStoppedSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor replier = actor {
            react {
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

            replier.send 'message'
        }

        latch.await()
        barrier.await()

        assert flag.get()
    }

    public void testReplyToReply() {
        String result

        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.actor {
            react {
                reply 'Message2'
                sender.send 'Message3'
                react { a ->
                    def senders = [sender]
                    react { b ->
                        senders << sender
                        senders*.send 'Message6'
                        latch.await()
                    }
                }
            }
        }

        Actors.actor {
            actor.send 'Message1'
            react {
                reply 'Message4'
                react {
                    reply 'Message5'
                    react { a ->
                        react { b ->
                            result = a + b
                            latch.countDown()
                        }
                    }
                }
            }
        }

        latch.await()
        assert 'Message6Message6' == result
    }

    public void testReplyFromNoArgHandler() {
        String result

        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = actor {
            react {
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
        assert 'Message2' == result

    }

    public void testMultipleClientsWithReply() {
        final List<CountDownLatch> latches = [new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(2)]
        def issues = []

        final def bouncer = actor {
            latches[0].await()
            def senders = []
            react { a ->
                senders << sender
                react { b ->
                    senders << sender
                    react { c ->
                        senders << sender
                        senders.findAll { it?.isActive() }*.send 4
                        latches[1].countDown()

                        latches[2].await()
                        senders = []
                        react { x ->
                            senders << sender
                            react { y ->
                                senders << sender
                                react { z ->
                                    senders << sender
                                    try {
                                        senders.each { currentSender ->
                                            try {
                                                currentSender?.send 8
                                                if (currentSender == null) issues << new IllegalStateException('Sender is not known')
                                            } catch (IllegalStateException e) {
                                                issues << e
                                            }
                                        }
                                    } finally {
                                        latches[3].countDown()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //send and terminate
        final Actor actor1 = actor {
            delegate.metaClass.afterStop = {
                latches[0].countDown()
            }

            bouncer << 1
        }

        //wait, send and terminate
        final Actor actor2 = actor {
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
        assert 2 == issues.size()
        assert (issues[0] instanceof IllegalStateException) && (issues[1] instanceof IllegalStateException)
    }

    public void testOriginatorDetection() {
        final DataflowVariable originator1 = new DataflowVariable()
        final DataflowVariable originator2 = new DataflowVariable()
        final DataflowVariable originator3 = new DataflowVariable()

        final def bouncer = actor {
            react { msg1 ->
                originator1 << sender
                react { msg2 ->
                    originator2 << sender
                    react { msg3 ->
                        originator3 << sender
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
