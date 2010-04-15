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
public class GParsExecutorsPoolUtilTest extends GroovyTestCase {

    public void testAsyncClosure() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            def result = Collections.synchronizedSet(new HashSet())
            final CountDownLatch latch = new CountDownLatch(5);
            final Closure cl = {Number number -> result.add(number * 10); latch.countDown()}
            [1, 2, 3, 4, 5].each(cl.async())
            latch.await()
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testCallParallel() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            def resultA = 0, resultB = 0
            final CountDownLatch latch = new CountDownLatch(2)
            GParsExecutorsPoolUtil.callAsync({number -> resultA = number; latch.countDown()}, 2)
            GParsExecutorsPoolUtil.callAsync({number -> resultB = number; latch.countDown()}, 3)
            latch.await()
            assertEquals 2, resultA
            assertEquals 3, resultB
        }
    }

    public void testCallParallelWithResult() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            assertEquals 6, GParsExecutorsPoolUtil.callAsync({it * 2}, 3).get()
        }
    }

    public void testAsync() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            def resultA = 0, resultB = 0
            final CountDownLatch latch = new CountDownLatch(2);
            GParsExecutorsPoolUtil.async({int number -> resultA = number; latch.countDown()}).call(2);
            GParsExecutorsPoolUtil.async({int number -> resultB = number; latch.countDown()}).call(3);
            latch.await()
            assertEquals 2, resultA
            assertEquals 3, resultB
        }
    }

    public void testAsyncWithResult() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            assertEquals 6, GParsExecutorsPoolUtil.async({it * 2}).call(3).get()
        }
    }

    public void testInvalidPoolSize() {
        shouldFail(IllegalArgumentException.class) {
            GParsExecutorsPool.withPool(0) {}
        }
        shouldFail(IllegalArgumentException.class) {
            GParsExecutorsPool.withPool(-10) {}
        }
    }

    public void testMissingThreadFactory() {
        shouldFail(IllegalArgumentException.class) {
            GParsExecutorsPool.withPool(5, null) {}
        }
    }

    public void testMissingThreadPool() {
        final AtomicInteger counter = new AtomicInteger(0)
        shouldFail(IllegalStateException.class) {
            GParsExecutorsPoolUtil.callAsync({counter.set it}, 1)
        }
        assertEquals 0, counter.get()
    }

    public void testLeftShift() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final Semaphore semaphore = new Semaphore(0)
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            service << {flag.set(true); semaphore.release(); }
            semaphore.acquire()
            assert flag.get()
        }
    }

    public testNestedCalls() {
        GParsExecutorsPool.withPool(5) {pool ->
            def result = ['abc', '123', 'xyz'].findAllParallel {word ->
                GParsExecutorsPool.withExistingPool(pool) {
                    word.anyParallel {it in ['a', 'y', '5']}
                }
            }
            assertEquals(['abc', 'xyz'], result)
        }
    }

    public void testNestedPools() {
        GParsExecutorsPool.withPool {a ->
            GParsExecutorsPool.withPool {b ->
                GParsExecutorsPool.withPool {c ->
                    GParsExecutorsPool.withPool {d ->
                        assert d != c != b != a
                        assert GParsExecutorsPool.retrieveCurrentPool() == d
                    }
                    assert GParsExecutorsPool.retrieveCurrentPool() == c
                }
                assert GParsExecutorsPool.retrieveCurrentPool() == b
            }
            assert GParsExecutorsPool.retrieveCurrentPool() == a
        }
    }

    public void testNestedExistingPools() {
        final def pool1 = Executors.newFixedThreadPool(1)
        final def pool2 = Executors.newFixedThreadPool(1)
        final def pool3 = Executors.newFixedThreadPool(1)
        GParsExecutorsPool.withExistingPool(pool1) {a ->
            GParsExecutorsPool.withExistingPool(pool2) {b ->
                GParsExecutorsPool.withExistingPool(pool1) {c ->
                    GParsExecutorsPool.withExistingPool(pool3) {d ->
                        assert d == pool3
                        assert c == pool1
                        assert b == pool2
                        assert a == pool1
                        assert GParsExecutorsPool.retrieveCurrentPool() == pool3
                    }
                    assert GParsExecutorsPool.retrieveCurrentPool() == pool1
                }
                assert GParsExecutorsPool.retrieveCurrentPool() == pool2
            }
            assert GParsExecutorsPool.retrieveCurrentPool() == pool1
        }
    }
}
