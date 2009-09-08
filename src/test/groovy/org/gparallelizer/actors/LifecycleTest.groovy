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

/**
 *
 * @author Vaclav Pech
 * Date: Jan 12, 2009
 */
public class LifecycleTest extends GroovyTestCase {
    public void testOnInterrupt() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultThreadActor actor = Actors.actor {throw new InterruptedException('test')}
        actor.metaClass.onInterrupt = {
            exception=it
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof InterruptedException
    }

    public void testOnException() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultThreadActor actor = Actors.actor {throw new RuntimeException('test')}
        actor.metaClass.onException = {
            exception=it
            latch.countDown()
        }

        actor.metaClass.afterStop = {
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof RuntimeException
        assertEquals('test', exception.message)
        assert actor.isActive()
    }

    public void testOneShotOnInterrupt() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultThreadActor actor = Actors.oneShotActor {throw new InterruptedException('test')}
        actor.metaClass.onInterrupt = {
            exception=it
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof InterruptedException
    }

    public void testOnShotOnException() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultThreadActor actor = Actors.oneShotActor {throw new RuntimeException('test')}
        actor.metaClass.onException = {
            exception=it
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof RuntimeException
        assertEquals('test', exception.message)
        assert !actor.isActive()
    }

    public void testOnExceptionHandlerCanStopTheActor() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultThreadActor actor = Actors.actor {throw new RuntimeException('test')}
        actor.metaClass.onException = {
            exception=it
            stop()
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof RuntimeException
        assertEquals('test', exception.message)
        assert !actor.isActive()
    }

    public void testExceptionInOnExceptionHandlerStopsTheActor() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultThreadActor actor = Actors.actor {throw new RuntimeException('test')}
        actor.metaClass.onException = {
            exception=it
            throw new RuntimeException('testing failed handler')
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof RuntimeException
        assertEquals('test', exception.message)
        assert !actor.isActive()
    }
}
