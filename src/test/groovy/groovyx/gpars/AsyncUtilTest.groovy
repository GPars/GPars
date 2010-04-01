// GPars (formerly GParallelizer)
//
// Copyright © 2008-10  The original author or authors
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class AsyncUtilTest extends GroovyTestCase {

    public void testAsyncClosure() {
        ThreadPool.withPool(5) {ExecutorService service ->
            def result = Collections.synchronizedSet(new HashSet())
            final CountDownLatch latch = new CountDownLatch(5);
            final Closure cl = {Number number -> result.add(number * 10); latch.countDown()}
            [1, 2, 3, 4, 5].each(cl.async())
            latch.await()
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testCallParallel() {
        ThreadPool.withPool(5) {ExecutorService service ->
            def resultA = 0, resultB = 0
            final CountDownLatch latch = new CountDownLatch(2)
            AsyncInvokerUtil.callAsync({number -> resultA = number; latch.countDown()}, 2)
            AsyncInvokerUtil.callAsync({number -> resultB = number; latch.countDown()}, 3)
            latch.await()
            assertEquals 2, resultA
            assertEquals 3, resultB
        }
    }

    public void testCallParallelWithResult() {
        ThreadPool.withPool(5) {ExecutorService service ->
            assertEquals 6, AsyncInvokerUtil.callAsync({it * 2}, 3).get()
        }
    }

    public void testAsync() {
        ThreadPool.withPool(5) {ExecutorService service ->
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
        ThreadPool.withPool(5) {ExecutorService service ->
            assertEquals 6, AsyncInvokerUtil.async({it * 2}).call(3).get()
        }
    }

    public void testInvalidPoolSize() {
        shouldFail(IllegalArgumentException.class) {
            ThreadPool.withPool(0) {}
        }
        shouldFail(IllegalArgumentException.class) {
            ThreadPool.withPool(-10) {}
        }
    }

    public void testMissingThreadFactory() {
        shouldFail(IllegalArgumentException.class) {
            ThreadPool.withPool(5, null) {}
        }
    }

    public void testMissingThreadPool() {
        final AtomicInteger counter = new AtomicInteger(0)
        shouldFail(IllegalStateException.class) {
            AsyncInvokerUtil.callAsync({counter.set it}, 1)
        }
        assertEquals 0, counter.get()
    }

    public void testLeftShift() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final Semaphore semaphore = new Semaphore(0)
        ThreadPool.withPool(5) {ExecutorService service ->
            service << {flag.set(true); semaphore.release(); }
            semaphore.acquire()
            assert flag.get()
        }
    }

    public testNestedCalls() {
        ThreadPool.withPool(5) {pool ->
            def result = ['abc', '123', 'xyz'].findAllParallel {word ->
                ThreadPool.withExistingPool(pool) {
                    word.anyParallel {it in ['a', 'y', '5']}
                }
            }
            assertEquals(['abc', 'xyz'], result)
        }
    }

    public void testNestedPools() {
        ThreadPool.withPool {a ->
            ThreadPool.withPool {b ->
                ThreadPool.withPool {c ->
                    ThreadPool.withPool {d ->
                        assert d != c != b != a
                        assert ThreadPool.retrieveCurrentPool() == d
                    }
                    assert ThreadPool.retrieveCurrentPool() == c
                }
                assert ThreadPool.retrieveCurrentPool() == b
            }
            assert ThreadPool.retrieveCurrentPool() == a
        }
    }

    public void testNestedExistingPools() {
        final def pool1 = Executors.newFixedThreadPool(1)
        final def pool2 = Executors.newFixedThreadPool(1)
        final def pool3 = Executors.newFixedThreadPool(1)
        ThreadPool.withExistingPool(pool1) {a ->
            ThreadPool.withExistingPool(pool2) {b ->
                ThreadPool.withExistingPool(pool1) {c ->
                    ThreadPool.withExistingPool(pool3) {d ->
                        assert d == pool3
                        assert c == pool1
                        assert b == pool2
                        assert a == pool1
                        assert ThreadPool.retrieveCurrentPool() == pool3
                    }
                    assert ThreadPool.retrieveCurrentPool() == pool1
                }
                assert ThreadPool.retrieveCurrentPool() == pool2
            }
            assert ThreadPool.retrieveCurrentPool() == pool1
        }
    }
}
