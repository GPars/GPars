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

package groovyx.gpars.actor.nonBlocking

import groovy.time.TimeCategory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import static groovyx.gpars.actor.Actors.actor

/**
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public class TimeCategoryTimeoutTest extends GroovyTestCase {
    protected void setUp() {
        super.setUp();
    }

    public void testTimeout() {
        final def barrier = new CyclicBarrier(2)
        final AtomicBoolean codeFlag = new AtomicBoolean(false)
        final AtomicBoolean timeoutFlag = new AtomicBoolean(false)

        actor {
            delegate.metaClass {
                onTimeout = {-> timeoutFlag.set(true); terminate() }
                afterStop = {messages -> barrier.await() }
            }

            loop {
                use(TimeCategory) {
                    react(1.second) {
                        codeFlag.set(true)  //should never reach
                    }
                }
            }
        }


        barrier.await()
        assertFalse codeFlag.get()
        assert timeoutFlag.get()
    }

    public void testTimeCategoryNotAvailable() {
        def exceptions = 0
        final CountDownLatch latch = new CountDownLatch(1)

        final def actor = actor {
            try {
                react(1.second) {}
            } catch (MissingPropertyException ignore) {exceptions++ }
            loop {
                try {
                    try {
                        react(1.minute) {}
                    } catch (MissingPropertyException ignore) {exceptions++ }
                    terminate()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        assert 2 == exceptions
    }

    public void testMessageBeforeTimeout() {
        final def barrier = new CyclicBarrier(2)
        final AtomicBoolean codeFlag = new AtomicBoolean(false)
        final AtomicBoolean nestedCodeFlag = new AtomicBoolean(false)
        final AtomicBoolean timeoutFlag = new AtomicBoolean(false)

        final def actor = actor {
            loop {
                use(TimeCategory) {
                    barrier.await()
                    react(5000.milliseconds) {
                        use(TimeCategory) {
                            codeFlag.set(true)
                            react(1.second) {
                                nestedCodeFlag.set(true)
                                barrier.await()
                                terminate()
                            }
                        }
                    }
                }
            }
        }

        actor.metaClass {
            onTimeout = {-> timeoutFlag.set(true) }
        }

        actor.send 'message'
        barrier.await()

        barrier.await()
        assert codeFlag.get()
        assert nestedCodeFlag.get()
        assert timeoutFlag.get()
    }

    public void testTimeoutInLoop() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger codeCounter = new AtomicInteger(0)
        final AtomicBoolean timeoutFlag = new AtomicBoolean(false)

        final def actor = actor {
            Integer counter = 0
            loop {
                use(TimeCategory) {
                    barrier.await()
                    counter++
                    if (counter == 3) terminate()
                    else react(5.seconds) {
                        codeCounter.incrementAndGet()
                    }
                }
            }
        }

        actor.metaClass {
            onTimeout = {-> timeoutFlag.set(true) }
        }

        actor.send 'message'
        barrier.await()
        barrier.await()

        barrier.await()
        assert 2 == codeCounter.get()
        assert timeoutFlag.get()
    }

    public void testTimeoutInLoopWithTermination() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger codeCounter = new AtomicInteger(0)

        final def actor = actor {
            loop {
                use(TimeCategory) {
                    barrier.await()
                    react(1.second) {
                        codeCounter.incrementAndGet()
                    }
                }
            }
        }

        actor.metaClass {
            onTimeout = {-> barrier.await(); terminate() }
        }

        actor.send 'message'
        barrier.await()
        barrier.await()

        barrier.await()
        actor.join()
        assert 1 == codeCounter.get()
        assert !actor.isActive()
    }
}
