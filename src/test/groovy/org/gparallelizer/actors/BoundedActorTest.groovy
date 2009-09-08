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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.gparallelizer.actors.BoundThreadActor

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */

public class BoundActorTest extends GroovyTestCase{
    public void testDefaultMessaging() {
        BoundTestActor actor=new BoundTestActor(2)
        actor.start()
        actor.send "Message"
        actor.latch.await(30, TimeUnit.SECONDS)
        assert actor.flag.get()
    }
}

class BoundTestActor extends BoundThreadActor {
    final AtomicBoolean flag = new AtomicBoolean(false)
    final CountDownLatch latch = new CountDownLatch(1)

    def BoundTestActor(final int capacity) {
        super(capacity);
    }


    @Override protected void act() {
        receive {
            flag.set true
            latch.countDown()

            stop()
        }
    }

    public String getThreadName() {
        return getActorThread().name
    }
}
