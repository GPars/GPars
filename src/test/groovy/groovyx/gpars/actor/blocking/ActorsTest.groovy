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

import groovyx.gpars.actor.Actors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * @author Vaclav Pech
 * Date: Jan 9, 2009
 */
public class ActorsTest extends GroovyTestCase {
    public void testDefaultActor() {
        final AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        Actors.blockingActor {
            while (true) {
                final int value = counter.incrementAndGet()
                if (value == 3) {
                    latch.countDown()
                    terminate()
                }
            }
        }

        latch.await(90, TimeUnit.SECONDS)
        assert 3 == counter.intValue()
    }

    public void testActorStopAfterTimeout() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        Actors.blockingActor {
            delegate.metaClass.afterStop = {
                latch.countDown()
            }

            receive(10, TimeUnit.MILLISECONDS)
            flag.set(true)
        }

        latch.await(90, TimeUnit.SECONDS)
        assert flag.get()
    }

    public void testDefaultActorWithException() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        Actors.actor {
            delegate.metaClass.onException = {}
            delegate.metaClass.afterStop = {
                flag.set(true)
                latch.countDown()
            }

            throw new RuntimeException('test')
        }

        latch.await(90, TimeUnit.SECONDS)
        assert flag.get()
    }
}