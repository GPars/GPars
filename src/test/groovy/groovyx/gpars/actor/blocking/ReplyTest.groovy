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
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.group.DefaultPGroup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean
import static groovyx.gpars.actor.Actors.oldActor

/**
 *
 * @author Vaclav Pech
 * Date: Apr 16, 2009
 */
public class ReplyTest extends GroovyTestCase {

    //todo enable - NPE
    public void _testMultipleClients() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def bouncer = Actors.oldActor {
            while (true) {
                receive {
                    reply it
                    barrier.await()
                }
            }
        }

        Thread.sleep 1000

        oldActor {
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

        oldActor {
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

        assertEquals([1, 2], replies1)
        assertEquals([10, 20], replies2)
    }

    public void testMultipleActors() {
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        final def group = new DefaultPGroup(5)
        def replies1 = []
        def replies2 = []

        final def incrementor = group.oldActor {
            while (true) { receive { reply it + 1 }}
        }

        final def decrementor = group.oldActor {
            while (true) { receive { reply it - 1 }}
        }

        group.oldActor {
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

        group.oldActor {
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
        assertEquals 4, replies1.size()
        assert replies1.containsAll([3, 5, 4, 8])
        assertEquals 4, replies2.size()
        assert replies2.containsAll([21, 59, 31, 39])
        group.shutdown()
    }

    public void testReplyWithoutSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = Actors.oldActor {
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

        actor.send 'messsage'
        barrier.await()

        actor.stop()

        assert flag.get()
    }

    public void testReplyIfExists() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor receiver = oldActor {
            receive {
                replyIfExists it
            }
        }

        oldActor {
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

        final Actor actor = oldActor {
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

        final Actor replier = oldActor {
            receive {
                latch.await()
                replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        oldActor {
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

        final Actor actor = Actors.oldActor {
            receive {
                reply 'Message2'
                reply 'Message3'
                receive {a ->
                    reply 'Message6'
                    receive { b ->
                        reply 'Message6'
                        latch.await()
                    }
                }
            }
        }

        Actors.oldActor {
            actor.send 'Message1'
            receive {
                reply 'Message4'
                receive {
                    reply 'Message5'
                    receive {a ->
                        receive {b ->
                            result = a + b
                            latch.countDown()
                        }
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

        final Actor actor = Actors.oldActor {
            receive {->
                reply 'Message2'
            }
        }

        Actors.oldActor {
            actor.send 'Message1'
            receive {
                result = it
                latch.countDown()
            }

        }

        latch.await()
        assertEquals 'Message2', result

    }

    //todo enable
    public void _testOriginatorDetection() {
        final DataFlowVariable originator1 = new DataFlowVariable()
        final DataFlowVariable originator2 = new DataFlowVariable()
        final DataFlowVariable originator3 = new DataFlowVariable()
        final DataFlowVariable originator4 = new DataFlowVariable()

        final def bouncer = oldActor {
            receive {msg1 ->
                originator1 << sender
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

        final def actor1 = oldActor {
            bouncer << 'msg1'
        }

        assert actor1 == originator1.val

        final def actor2 = oldActor {
            bouncer << 'msg2'
        }

        assert actor2 == originator2.val

        bouncer << 'msg3'

        assertNull originator3.val

        final def actor4 = oldActor {
            bouncer << 'msg4'
        }

        assert actor4 == originator4.val
    }
}
