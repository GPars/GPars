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

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actor

public class PooledActorThreadTest extends GroovyTestCase {
    public void testActorThread() {
        volatile boolean flag1 = false
        volatile boolean flag2 = false
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor
        actor = PooledActors.actor {
            flag1=isActorThread()
            react {
                flag2=isActorThread()
                latch.countDown()
            }

        }.start()

        assertFalse actor.isActorThread()
        actor.send 'Message'
        latch.await()
        assert flag1
        assert flag2
    }
}
