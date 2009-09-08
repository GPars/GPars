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

package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import static org.gparallelizer.actors.pooledActors.PooledActors.actor
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.atomic.AtomicReference

/**
 *
 * @author Vaclav Pech
 * Date: Feb 17, 2009
 */
public class LoopTest extends GroovyTestCase {

    protected void setUp() {
        super.setUp();
        PooledActors.defaultPooledActorGroup.resize(10)
    }

    public void testLoop() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final AbstractPooledActor actor = actor {
            loop {
                react {
                    counter.incrementAndGet()
                    barrier.await()
                    react {message ->
                        counter.incrementAndGet()
                        barrier.await()

                    }
                }
            }
        }.start()

        Thread.sleep 1000
        assertEquals 0, counter.intValue()

        1.upto(7) {
            actor.send 'message'
            barrier.await()
            assertEquals it, counter.intValue()
        }
        actor.stop()
    }

    public void testLoopStop() {
        final def barrier = new CyclicBarrier(2)
        final def afterStopBarrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final AbstractPooledActor actor = actor {
            loop {
                barrier.await()
                Thread.sleep 10000
                react {
                    counter.incrementAndGet()
                }
            }
        }.start()

        actor.metaClass {
            afterStop = {List messages ->
                afterStopBarrier.await()
            }
            onInterrupt = {}
        }

        barrier.await()
        actor.send 'message'
        actor.stop()
        afterStopBarrier.await()
        assertEquals 0, counter.intValue()
    }

    public void testSubsequentLoopStop() {
        final def barrier = new CyclicBarrier(2)
        final def afterBarrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)
        AtomicReference<List> messagesReference = new AtomicReference<List>(null)

        final AbstractPooledActor actor = actor {
            loop {
                barrier.await()
                react {
                    counter.incrementAndGet()
                    barrier.await()
                    Thread.sleep 10000
                }
            }
        }.start()

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                afterBarrier.await()
            }
            onInterrupt = {}
        }

        actor.send 'message'
        barrier.await()
        actor.send 'message'
        barrier.await()
        actor.stop()

        afterBarrier.await()
        assertEquals 1, counter.intValue()
        assertEquals 1, messagesReference.get().size()
    }

    public void testBeforeLoopStop() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final AbstractPooledActor actor = actor {
            Thread.sleep 10000
            loop {
                counter.incrementAndGet()
            }
        }.start()

        actor.metaClass { onInterrupt ={} }

        actor.send 'message'
        actor.stop()

        Thread.sleep 1000
        assertEquals 0, counter.intValue()
    }
}
