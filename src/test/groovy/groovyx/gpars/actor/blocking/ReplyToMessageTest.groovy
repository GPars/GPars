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
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.DefaultPool
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean

public class ReplyToMessageTest extends GroovyTestCase {

    def group

    protected void setUp() {
        super.setUp();
        group = new DefaultPGroup(new DefaultPool(true, 10))
    }

    protected void tearDown() {
        super.tearDown();
        group.shutdown()
    }

    public void testMultipleClients() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def bouncer = group.oldActor {
            loop {
                receive {
                    it.reply it
                    barrier.await()
                }
            }
        }

        Thread.sleep 1000

        group.oldActor {
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

        group.oldActor {
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
        def replies1 = []
        def replies2 = []

        final def incrementor = group.oldActor {
            loop { receive { it.reply it + 1 }}
        }

        final def decrementor = group.oldActor {
            loop { receive { it.reply it - 1 }}
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
    }

    public void testReplyWithoutSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = group.oldActor {
            delegate.metaClass {
                onException = {
                    flag.set(true)
                    barrier.await()
                }
            }

            loop {
                receive {
                    it.reply it
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

        final Actor receiver = group.oldActor {
            receive {
                it.replyIfExists it
            }
        }

        group.oldActor {
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

        final Actor actor = group.oldActor {
            receive {
                it.replyIfExists it
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

        final Actor replier = group.oldActor {
            receive {
                latch.await()
                it.replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        group.oldActor {
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

    public void testNestedReplies() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def maxFinder = group.oldActor {
            barrier.await()
            receive {message1 ->
                receive {message2 ->
                    def max = Math.max(message1, message2)
                    message1.reply max
                    message2.reply max
                }
            }
        }


        group.oldActor {
            barrier.await()
            maxFinder.send 2
            receive {
                replies1 << it
                completedBarrier.await()
            }
        }

        group.oldActor {
            barrier.await()
            maxFinder.send 3
            receive {
                replies2 << it
                completedBarrier.await()
            }
        }

        completedBarrier.await()
        assertEquals 1, replies1.size()
        assertEquals 1, replies2.size()

        assert replies1.contains(3)
        assert replies2.contains(3)
    }

}
