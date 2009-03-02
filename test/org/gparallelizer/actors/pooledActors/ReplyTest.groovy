package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CyclicBarrier
import static org.gparallelizer.actors.pooledActors.PooledActors.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CountDownLatch

/**
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public class ReplyTest extends GroovyTestCase {

    protected void setUp() {
        super.setUp();
        PooledActors.pool.resize(5)
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
        }.start()

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
        }.start()

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
            loop {
                react { reply it + 1 }
            }
        }.start()

        final def decrementor = actor {
            loop {
                react { reply it - 1 }
            }
        }.start()

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
        }.start()

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
        }.start()

        completedBarrier.await()
        assertEquals 4, replies1.size()
        assert replies1.containsAll([3, 5, 4, 8])
        assertEquals 4, replies2.size()
        assert replies2.containsAll([21, 59, 31, 39])
    }

    public void testReplyWithoutSender() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final AbstractPooledActor actor = actor {
            react {
                reply it
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

        receiver.start()

        actor {
            receiver.send 'messsage'
            react {
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

        final AbstractPooledActor actor = actor {
            react {
                replyIfExists it
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

        final AbstractPooledActor replier = actor {
            react {
                latch.await()
                replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        replier.start()

        final AbstractPooledActor sender = actor {
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
}