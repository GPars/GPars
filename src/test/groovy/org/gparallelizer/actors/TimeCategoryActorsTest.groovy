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

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 *
 * @author Vaclav Pech
 * Date: Jan 16, 2009
 */

public class TimeCategoryActorsTest extends GroovyTestCase {
    public void testReceive() {
        volatile def result=''
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
            result = receive(3.seconds)
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assertNull(result)
    }

    public void testReceiveWithHandler() {
        volatile def result=''
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
            receive(2.seconds) {
                result = it
            }
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assertNull(result)
    }
}
