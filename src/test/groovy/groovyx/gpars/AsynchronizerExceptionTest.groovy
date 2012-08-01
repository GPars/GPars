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

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Vaclav Pech
 * Date: Nov 17, 2008
 */
public class ThreadPoolExceptionTest extends GroovyTestCase {
    public void testDoInParralelWithException() {
        shouldFail {
            GParsExecutorsPoolUtil.executeAsyncAndWait({20}, {throw new RuntimeException('test1')}, {throw new RuntimeException('test2')}, {10})
        }
    }

    public void testExecuteInParralelWithException() {
        GParsExecutorsPool.withPool {
            List<Future<Object>> result = GParsExecutorsPool.executeAsync({20}, {throw new RuntimeException('test1')}, {throw new RuntimeException('test2')}, {10})
            shouldFail {
                result*.get()
            }
        }
    }

    public void testThreadException() {
        final AtomicReference thrownException = new AtomicReference()
        final def latch = new CountDownLatch(1)
        final def thread = new Thread({throw new RuntimeException('test')} as Runnable)
        thread.uncaughtExceptionHandler = {t, throwable -> thrownException.set(throwable); latch.countDown()} as UncaughtExceptionHandler
        thread.start()
        latch.await()
        assert thrownException.get() instanceof RuntimeException
    }

    public void testThreadPoolException() {
        ExecutorService pool = Executors.newFixedThreadPool(2, {
            final def thread = new Thread(it as Runnable)
            thread.uncaughtExceptionHandler = {t, throwable -> thrownException = throwable; latch.countDown()} as UncaughtExceptionHandler
            thread.daemon = false
            thread
        } as ThreadFactory)
        def future = pool.submit {throw new RuntimeException('test')} as Runnable
        try {
            future.get()
        } catch (Exception e) {
            assert e instanceof ExecutionException
            assert e.cause instanceof RuntimeException
        }
        pool.shutdown()
    }

    public void testEachWithException() {
        shouldFail(AsyncException.class) {
            GParsExecutorsPool.withPool(5) {ExecutorService service ->
                'abc'.eachParallel {throw new RuntimeException('test')}
            }
        }
    }

    public void testCollectWithException() {
        shouldFail(AsyncException.class) {
            GParsExecutorsPool.withPool(5) {ExecutorService service ->
                'abc'.collectParallel {if (it == 'b') throw new RuntimeException('test')}
            }
        }
    }

    public void testFindAllWithException() {
        shouldFail(AsyncException.class) {
            GParsExecutorsPool.withPool(5) {ExecutorService service ->
                'abc'.findAllParallel {if (it == 'b') throw new RuntimeException('test') else return true}
            }
        }
    }

    public void testFindWithException() {
        shouldFail(AsyncException.class) {
            GParsExecutorsPool.withPool(5) {ExecutorService service ->
                'abc'.findParallel {if (it == 'b') throw new RuntimeException('test') else return true}
            }
        }
    }

    public void testAllWithException() {
        shouldFail(AsyncException.class) {
            GParsExecutorsPool.withPool(5) {ExecutorService service ->
                'abc'.everyParallel {if (it == 'b') throw new RuntimeException('test') else return true}
            }
        }
    }

    public void testAnyWithException() {
        shouldFail(AsyncException.class) {
            GParsExecutorsPool.withPool(5) {ExecutorService service ->
                'abc'.anyParallel {if (it == 'b') throw new RuntimeException('test') else return false}
            }
        }
    }
}
