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

import groovyx.gpars.actor.BlockingActor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Vaclav Pech, Dierk Koenig
 * Date: Jan 7, 2009
 */
public class DefaultActorTest extends GroovyTestCase {
    public void testDefaultMessaging() {
        DefaultTestActor actor = new DefaultTestActor()
        actor.start()
        actor.send "Message"
        actor.receiveCallsOutstanding.await(90, TimeUnit.SECONDS)
        assert actor.receiveWasCalled.get()
    }

    public void testThreadName() {
        DefaultTestActor actor = new DefaultTestActor()
        actor.start()
        actor << ''
        actor.receiveCallsOutstanding.await(90, TimeUnit.SECONDS)

        assert actor.threadName.startsWith("Actor Thread ")
        actor.terminate()
    }
}

class DefaultTestActor extends BlockingActor {

    final AtomicBoolean receiveWasCalled = new AtomicBoolean(false)
    final CountDownLatch receiveCallsOutstanding = new CountDownLatch(1)

    volatile def threadName = ''

    @Override protected void act() {
        threadName = Thread.currentThread().name
        receive {
            receiveWasCalled.set true
            receiveCallsOutstanding.countDown()

            terminate()
        }
    }
}
