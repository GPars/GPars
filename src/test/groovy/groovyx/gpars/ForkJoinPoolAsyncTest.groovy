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

package groovyx.gpars

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */

public class ForkJoinPoolAsyncTest extends GroovyTestCase {

    public void testAsyncClosure() {
        GParsPool.withPool(5) {service ->
            def result = Collections.synchronizedSet(new HashSet())
            final CountDownLatch latch = new CountDownLatch(5);
            final Closure cl = {Number number -> result.add(number * 10); latch.countDown()}
            [1, 2, 3, 4, 5].each(cl.async())
            latch.await()
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testCallParallel() {
        GParsPool.withPool(5) {service ->
            def resultA = 0, resultB = 0
            final CountDownLatch latch = new CountDownLatch(2)
            GParsPoolUtil.callAsync({number -> resultA = number; latch.countDown()}, 2)
            GParsPoolUtil.callAsync({number -> resultB = number; latch.countDown()}, 3)
            latch.await()
            assertEquals 2, resultA
            assertEquals 3, resultB
        }
    }

    public void testCallParallelWithResult() {
        GParsPool.withPool(5) {service ->
            assertEquals 6, GParsPoolUtil.callAsync({it * 2}, 3).get()
        }
    }

    public void testAsync() {
        GParsPool.withPool(5) {service ->
            def resultA = 0, resultB = 0
            final CountDownLatch latch = new CountDownLatch(2);
            GParsPoolUtil.async({int number -> resultA = number; latch.countDown()}).call(2);
            GParsPoolUtil.async({int number -> resultB = number; latch.countDown()}).call(3);
            latch.await()
            assertEquals 2, resultA
            assertEquals 3, resultB
        }
    }

    public void testAsyncWithResult() {
        GParsPool.withPool(5) {service ->
            assertEquals 6, GParsPoolUtil.async({it * 2}).call(3).get()
        }
    }

    public void testInvalidPoolSize() {
        shouldFail(IllegalArgumentException.class) {
            GParsPool.withPool(0) {}
        }
        shouldFail(IllegalArgumentException.class) {
            GParsPool.withPool(-10) {}
        }
    }

    public void testMissingThreadPool() {
        final AtomicInteger counter = new AtomicInteger(0)
        shouldFail(IllegalStateException.class) {
            GParsPoolUtil.callAsync({counter.set it}, 1)
        }
        assertEquals 0, counter.get()
    }

    public void testLeftShift() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final Semaphore semaphore = new Semaphore(0)
        GParsPool.withPool(5) {service ->
            service << {flag.set(true); semaphore.release(); }
            semaphore.acquire()
            assert flag.get()
        }
    }

    public void testDoInParallel() {
        GParsPool.withPool {
            assertEquals([10, 20], GParsPool.executeAsyncAndWait({10}, {20}))
        }
    }

    public void testExecuteInParallel() {
        GParsPool.withPool {
            assertEquals([10, 20], GParsPool.executeAsync({10}, {20})*.get())
        }
    }

    public void testDoInParallelList() {
        GParsPool.withPool {
            assertEquals([10, 20], GParsPool.executeAsyncAndWait([{10}, {20}]))
        }
    }

    public void testExecuteAsyncList() {
        GParsPool.withPool {
            assertEquals([10, 20], GParsPool.executeAsync([{10}, {20}])*.get())
        }
    }

    public void testAsyncWithCollectionAndResult() {
        GParsPool.withPool(5) {service ->
            Collection<Future> result = [1, 2, 3, 4, 5].collect({it * 10}.async())
            assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection) result*.get()))
        }
    }
}
