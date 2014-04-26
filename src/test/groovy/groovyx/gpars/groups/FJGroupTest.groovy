// GPars - Groovy Parallel Systems
//
// Copyright © 2008--2011  The original author or authors
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

package groovyx.gpars.groups

import groovyx.gpars.actor.Actor
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.DefaultPool
import groovyx.gpars.scheduler.FJPool
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinWorkerThread

public class FJGroupTest extends GroovyTestCase {
    public void testFJGroup() {
        final DefaultPGroup group = new DefaultPGroup(new FJPool())

        final CountDownLatch latch = new CountDownLatch(1)
        boolean result = false

        group.actor {
            result = Thread.currentThread() instanceof ForkJoinWorkerThread
            latch.countDown()
        }

        latch.await()
        assert result
        group.shutdown()
    }

    public void testNonFJGroup() {
        final DefaultPGroup group = new DefaultPGroup(new DefaultPool())

        final CountDownLatch latch = new CountDownLatch(1)
        boolean result = false

        group.actor {
            result = Thread.currentThread() instanceof ForkJoinWorkerThread
            latch.countDown()
        }

        latch.await()
        assertFalse result
        group.shutdown()
    }

    public void testFJNonFJGroupCommunication() {
        final DefaultPGroup group1 = new DefaultPGroup(new DefaultPool())
        final DefaultPGroup group2 = new DefaultPGroup(new FJPool())

        final CountDownLatch latch = new CountDownLatch(1)
        int result = 0

        final Actor actor1 = group1.actor {
            react {
                reply it + 5
            }
        }

        final Actor actor2 = group2.actor {
            react {
                actor1 << it + 10
                react {message ->
                    result = message
                    latch.countDown()
                }
            }
        }

        actor2 << 10
        latch.await()
        assert 25 == result
        group1.shutdown()
        group2.shutdown()
    }
}
