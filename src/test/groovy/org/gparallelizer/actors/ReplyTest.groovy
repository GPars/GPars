package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean
import org.gparallelizer.actors.Actor
import static org.gparallelizer.actors.Actors.actor
import static org.gparallelizer.actors.Actors.oneShotActor

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

        final def bouncer = actor {
            receive {
                reply it
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
            receive { reply it + 1 }
        }.start()

        final def decrementor = actor {
            receive { reply it - 1 }
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
        actor.stop()

        assert flag.get()
    }

    public void testReplyIfExists() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor receiver = oneShotActor {
            receive {
                replyIfExists it
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

        final Actor replier = oneShotActor {
            receive {
                latch.await()
                replyIfExists it
                flag.set(true)
                barrier.await()
            }
        }

        replier.start()

        final Actor sender = oneShotActor { replier.send 'messsage' }

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

    public void testReplyToReply() {
        volatile String result

        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
            receive {
                reply 'Message2'
                it.reply 'Message3'
                receive {a, b->
                    reply 'Message6'
                    latch.await()
                }
            }
        }.start()

        Actors.oneShotActor{
            actor.send 'Message1'
            receive {
                it.reply 'Message4'
                receive {
                    reply 'Message5'
                    receive {a, b ->
                        result = a+b
                        latch.countDown()
                    }
                }
            }

        }.start()

        latch.await()
        assertEquals 'Message6Message6', result
    }

}