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

package org.gparallelizer.actors.groups

import java.util.concurrent.CountDownLatch
import jsr166y.forkjoin.ForkJoinWorkerThread
import org.gparallelizer.actors.AbstractThreadActorGroup
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.ThreadActorGroup

public class FJGroupTest extends GroovyTestCase {
    public void testFJGroup() {
        final AbstractThreadActorGroup group = new ThreadActorGroup(true)

        final CountDownLatch latch = new CountDownLatch(1)
        boolean result = false

        group.oneShotActor {
            result = Thread.currentThread() instanceof ForkJoinWorkerThread
            latch.countDown()
        }.start()

        latch.await()
        assert result
    }

    public void testNonFJGroup() {
        final AbstractThreadActorGroup group = new ThreadActorGroup(false)

        final CountDownLatch latch = new CountDownLatch(1)
        boolean result = false

        group.oneShotActor {
            result = Thread.currentThread() instanceof ForkJoinWorkerThread
            latch.countDown()
        }.start()

        latch.await()
        assertFalse result
    }

    public void testFJNonFJGroupCommunication() {
        final AbstractThreadActorGroup group1 = new ThreadActorGroup(false)
        final AbstractThreadActorGroup group2 = new ThreadActorGroup(true)

        final CountDownLatch latch = new CountDownLatch(1)
        int result = 0

        final Actor actor1 = group1.oneShotActor {
            receive {
                reply it + 5
            }
        }
        actor1.start()

        final Actor actor2 = group2.oneShotActor {
            receive {
                actor1 << it + 10
                receive {message ->
                    result = message
                    latch.countDown()
                }
            }
        }
        actor2.start()

        actor2 << 10
        latch.await()
        assertEquals 25, result
        group1.shutdown()
        group2.shutdown()
    }
}
