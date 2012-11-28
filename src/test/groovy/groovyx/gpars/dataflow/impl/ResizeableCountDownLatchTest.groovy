// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.dataflow.impl

import java.util.concurrent.TimeUnit

public class ResizeableCountDownLatchTest extends GroovyTestCase {

    public void testIncreaseCount() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(1)
        assert latch.count == 1
        latch.increaseCount()
        assert latch.count == 2
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.countDown()
        latch.countDown()
        assert latch.await(10, TimeUnit.MILLISECONDS)
    }

    public void testIncreaseCountAfterReachingZero() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(1)
        assert latch.count == 1
        latch.increaseCount()
        latch.countDown()
        assert latch.count == 1

        latch.increaseCount()
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.countDown()
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.countDown()
        assert latch.await(10, TimeUnit.MILLISECONDS)
    }

    @SuppressWarnings("GroovyMethodWithMoreThanThreeNegations")
    public void testGradualIncreaseCount() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(1)
        assert latch.count == 1
        latch.increaseCount()
        assert latch.count == 2
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.increaseCount()
        assert latch.count == 3
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.countDown()
        assert latch.count == 2
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.increaseCount()
        assert latch.count == 3
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.countDown()
        latch.countDown()
        latch.countDown()
        assert latch.await(10, TimeUnit.MILLISECONDS)
    }

    public void testDecreaseCount() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(1)
        assert latch.count == 1
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.decreaseCount()
        assert latch.count == 0
        assert latch.await(10, TimeUnit.MILLISECONDS)
    }

    public void testDecreaseCountAfterReachingZero() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(1)
        assert latch.count == 1
        latch.increaseCount()
        assert latch.count == 2
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.decreaseCount()
        latch.decreaseCount()
        assert latch.await(10, TimeUnit.MILLISECONDS)
    }

    public void testDecreaseBelowZero() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(1)
        assert latch.count == 1
        latch.decreaseCount()
        assert latch.count == 0
        assert latch.await(10, TimeUnit.MILLISECONDS)

        latch.decreaseCount()
        assert latch.count == -1
        assert !latch.await(10, TimeUnit.MILLISECONDS)
    }

    public void testSimpleAwaitWithTimeout() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(3)
        assert latch.count == 3
        latch.countDown()
        assert !latch.attemptToCountDownAndAwait(500000)
        assert latch.count == 3
        assert !latch.await(10, TimeUnit.MILLISECONDS)
    }

    public void testAwaitWithTimeoutWithManyParties() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(3)
        assert latch.count == 3
        latch.countDown()
        latch.countDown()
        latch.countDown()
        latch.countDown()
        latch.countDown()
        assert latch.attemptToCountDownAndAwait(500000)
        assert latch.count == 0
        assert latch.await(10, TimeUnit.MILLISECONDS)
    }

    public void testAwaitWithTimeoutWithTrickyScenario() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(3)
        assert latch.count == 3
        latch.countDown()
        latch.countDown()
        assert !latch.await(10, TimeUnit.MILLISECONDS)
        latch.countDown()

        latch.increaseCount()
        assert latch.count == 1
        latch.countDown()
        assert latch.isReleasedFlag()
        assert latch.await(10, TimeUnit.MILLISECONDS)
        assert latch.count == 0
    }

    public void testCompetingAwaitWithTimeout() throws Exception {
        final ResizeableCountDownLatch latch = new ResizeableCountDownLatch(3)
        assert latch.count == 3
        Thread.start {
            latch.countDown()
            latch.await()
        }
        Thread.start {
            latch.countDown()
            latch.await()
        }
        latch.countDown()
        assert latch.attemptToCountDownAndAwait(30000000)
        latch.increaseCount()
        assert latch.count == 1
        assert !latch.await(0, TimeUnit.MILLISECONDS)
    }
}
