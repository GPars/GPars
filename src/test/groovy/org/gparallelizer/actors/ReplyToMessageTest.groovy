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

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.CountDownLatch
import static org.gparallelizer.actors.Actors.*

public class ReplyToMessageTest extends GroovyTestCase {

    protected void setUp() {
        super.setUp();
        Actors.defaultActorGroup.resize 1
    }

    protected void tearDown() {
        super.tearDown();
        Actors.defaultActorGroup.resetDefaultSize()
    }

    public void testMultipleClients() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def bouncer = actor {
            receive {
                it.reply it
                barrier.await()
            }
        }.start()

        Thread.sleep 1000

        oneShotActor {
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
        }.start()

        oneShotActor {
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
        }.start()

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
            receive { it.reply it + 1 }
        }.start()

        final def decrementor = actor {
            receive { it.reply it - 1 }
        }.start()

        oneShotActor {
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
        }.start()

        oneShotActor {
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
        }.start()

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

        final Actor actor = actor {
            receive {
                it.reply it
            }
        }

        actor.metaClass {
            onException = {
                flag.set(true)
                barrier.await()
            }
        }

        actor.start()

        actor.send 'messsage'
        barrier.await()
        actor.stop()

        assert flag.get()
    }

    public void testReplyIfExists() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor receiver = oneShotActor {
            receive {
                it.replyIfExists it
            }
        }

        receiver.start()

        oneShotActor {
            receiver.send 'messsage'
            receive {
                flag.set(true)
                barrier.await()
            }
        }.start()

        barrier.await()

        assert flag.get()
    }

    public void testReplyIfExistsWithoutSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = oneShotActor {
            receive {
                it.replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        actor.start()

        actor.send 'messsage'
        barrier.await()

        assert flag.get()
    }

    public void testReplyIfExistsWithStoppedSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor replier = oneShotActor {
            receive {
                latch.await()
                it.replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        replier.start()

        final Actor sender = oneShotActor {
            replier.send 'messsage'
        }

        sender.metaClass {
            afterStop = {
                latch.countDown()
            }
        }

        sender.start()

        latch.await()
        barrier.await()

        assert flag.get()
    }

    public void testNestedReplies() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def maxFinder = oneShotActor {
            barrier.await()
            receive {message1 ->
                receive {message2 ->
                    def max = Math.max(message1, message2)
                    message1.reply max
                    message2.reply max
                }
            }
        }.start()


        oneShotActor {
            barrier.await()
            maxFinder.send 2
            receive {
                replies1 << it
                completedBarrier.await()
            }
        }.start()

        oneShotActor {
            barrier.await()
            maxFinder.send 3
            receive {
                replies2 << it
                completedBarrier.await()
            }
        }.start()

        completedBarrier.await()
        assertEquals 1, replies1.size()
        assertEquals 1, replies2.size()

        assert replies1.contains(3)
        assert replies2.contains(3)
    }
}
