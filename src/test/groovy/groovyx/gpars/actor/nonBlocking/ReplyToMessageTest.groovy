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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean

import static groovyx.gpars.actor.Actors.actor

public class ReplyToMessageTest extends GroovyTestCase {

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

    public void testNestedReplies() {
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CyclicBarrier completedBarrier = new CyclicBarrier(3)
        def replies1 = []
        def replies2 = []

        final def maxFinder = actor {
            barrier.await()
            react { message1 ->
                def sender1 = sender
                react { message2 ->
                    def max = Math.max(message1, message2)
                    sender1.send max
                    sender.send max
                }
            }
        }


        actor {
            barrier.await()
            maxFinder.send 2
            react {
                replies1 << it
                completedBarrier.await()
            }
        }

        actor {
            barrier.await()
            maxFinder.send 3
            react {
                replies2 << it
                completedBarrier.await()
            }
        }

        completedBarrier.await()
        assert 1 == replies1.size()
        assert 1 == replies2.size()

        assert replies1.contains(3)
        assert replies2.contains(3)
    }
}
