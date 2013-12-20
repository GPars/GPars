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

package groovyx.gpars.actor.blocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean

import static groovyx.gpars.actor.Actors.blockingActor

/**
 *
 * @author Vaclav Pech
 * Date: Apr 16, 2009
 */
public class ReplyTest extends GroovyTestCase {

    public void testMultipleClients() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def bouncer = Actors.blockingActor {
            while (true) {
                receive {
                    reply it
                    barrier.await()
                }
            }
        }

        Thread.sleep 1000

        blockingActor {
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

        blockingActor {
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
        bouncer.terminate()

        assert [1, 2] == replies1
        assert [10, 20] == replies2
    }

    public void testMultipleActors() {
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        final def group = new DefaultPGroup(5)
        def replies1 = []
        def replies2 = []

        final def incrementor = group.blockingActor {
            while (true) {
                receive { reply it + 1 }
            }
        }

        final def decrementor = group.blockingActor {
            while (true) {
                receive { reply it - 1 }
            }
        }

        group.blockingActor {
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

        group.blockingActor {
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
        incrementor.terminate()
        decrementor.terminate()
        assert 4 == replies1.size()
        assert replies1.containsAll([3, 5, 4, 8])
        assert 4 == replies2.size()
        assert replies2.containsAll([21, 59, 31, 39])
        group.shutdown()
    }

    public void testReplyWithoutSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.blockingActor {
            delegate.metaClass {
                onException = {
                    flag.set(true)
                    barrier.await()
                }
            }

            while (true) {
                receive {
                    reply it
                }
            }
        }

        actor.send 'message'
        barrier.await()

        actor.stop()

        assert flag.get()
    }

    public void testReplyIfExists() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor receiver = blockingActor {
            receive {
                replyIfExists it
            }
        }

        blockingActor {
            receiver.send 'message'
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

        final Actor actor = blockingActor {
            receive {
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

        final Actor replier = blockingActor {
            receive {
                latch.await()
                replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        blockingActor {
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

        final Actor actor = Actors.blockingActor {
            receive {
                reply 'Message2'
                reply 'Message3'
                receive { a ->
                    reply 'Message6'
                    receive { b ->
                        reply 'Message6'
                        latch.await()
                    }
                }
            }
        }

        Actors.blockingActor {
            actor.send 'Message1'
            receive {
                reply 'Message4'
                receive {
                    reply 'Message5'
                    receive { a ->
                        receive { b ->
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

        final Actor actor = Actors.blockingActor {
            receive { ->
                reply 'Message2'
            }
        }

        Actors.blockingActor {
            actor.send 'Message1'
            receive {
                result = it
                latch.countDown()
            }

        }

        latch.await()
        assert 'Message2' == result

    }

    public void testOriginatorDetection() {
        final DataflowVariable originator1 = new DataflowVariable()
        final DataflowVariable originator2 = new DataflowVariable()
        final DataflowVariable originator3 = new DataflowVariable()
        final DataflowVariable originator4 = new DataflowVariable()

        final def bouncer = blockingActor {
            receive { msg1 ->
                originator1 << sender
                receive { msg2 ->
                    originator2 << sender
                    receive { msg3 ->
                        originator3 << sender
                    }
                    def msg4 = receive()
                    originator4 << sender
                }
            }
        }

        final def actor1 = blockingActor {
            bouncer << 'msg1'
        }

        assert actor1 == originator1.val

        final def actor2 = blockingActor {
            bouncer << 'msg2'
        }

        assert actor2 == originator2.val

        bouncer << 'msg3'

        assertNull originator3.val

        final def actor4 = blockingActor {
            bouncer << 'msg4'
        }

        assert actor4 == originator4.val
    }
}
