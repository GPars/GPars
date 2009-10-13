//  GPars (formerly GParallelizer)
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

package groovyx.gpars

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class AsyncUtilTest extends GroovyTestCase {

    public void testAsyncClosure() {
        Asynchronizer.doAsync(5) {ExecutorService service ->
            def result = Collections.synchronizedSet(new HashSet())
            final CountDownLatch latch = new CountDownLatch(5);
            final Closure cl = {Number number -> result.add(number * 10); latch.countDown()}
            [1, 2, 3, 4, 5].each(cl.async())
            latch.await()
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testCallParallel() {
        Asynchronizer.doAsync(5) {ExecutorService service ->
            def resultA = 0, resultB = 0
            final CountDownLatch latch = new CountDownLatch(2)
            AsyncInvokerUtil.callParallel({number -> resultA = number; latch.countDown()}, 2)
            AsyncInvokerUtil.callParallel({number -> resultB = number; latch.countDown()}, 3)
            latch.await()
            assertEquals 2, resultA
            assertEquals 3, resultB
        }
    }

    public void testCallParallelWithResult() {
        Asynchronizer.doAsync(5) {ExecutorService service ->
            assertEquals 6, AsyncInvokerUtil.callParallel({it * 2}, 3).get()
        }
    }

    public void testAsync() {
        Asynchronizer.doAsync(5) {ExecutorService service ->
            def resultA = 0, resultB = 0
            final CountDownLatch latch = new CountDownLatch(2);
            AsyncInvokerUtil.async({int number -> resultA = number; latch.countDown()}).call(2);
            AsyncInvokerUtil.async({int number -> resultB = number; latch.countDown()}).call(3);
            latch.await()
            assertEquals 2, resultA
            assertEquals 3, resultB
        }
    }

    public void testAsyncWithResult() {
        Asynchronizer.doAsync(5) {ExecutorService service ->
            assertEquals 6, AsyncInvokerUtil.async({it * 2}).call(3).get()
        }
    }

    public void testInvalidPoolSize() {
        shouldFail(IllegalArgumentException.class) {
            Asynchronizer.doAsync(0) {}
        }
        shouldFail(IllegalArgumentException.class) {
            Asynchronizer.doAsync(-10) {}
        }
    }

    public void testMissingThreadFactory() {
        shouldFail(IllegalArgumentException.class) {
            Asynchronizer.doAsync(5, null) {}
        }
    }

    public void testMissingAsynchronizer() {
        final AtomicInteger counter = new AtomicInteger(0)
        shouldFail(IllegalStateException.class) {
            AsyncInvokerUtil.callParallel({counter.set it}, 1)
        }
        assertEquals 0, counter.get()
    }

    public void testLeftShift() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final Semaphore semaphore = new Semaphore(0)
        Asynchronizer.doAsync(5) {ExecutorService service ->
            service << {flag.set(true); semaphore.release(); }
            semaphore.acquire()
            assert flag.get()
        }
    }

    public testNestedCalls() {
        Asynchronizer.doAsync(5) {pool ->
            def result = ['abc', '123', 'xyz'].findAllParallel {word ->
                Asynchronizer.doAsync(pool) {
                    word.anyParallel {it in ['a', 'y', '5']}
                }
            }
            assertEquals(['abc', 'xyz'], result)
        }
    }
}
