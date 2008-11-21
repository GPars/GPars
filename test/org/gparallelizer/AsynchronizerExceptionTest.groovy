package org.gparallelizer

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Vaclav Pech
 * Date: Nov 17, 2008
 */
public class AsynchronizerExceptionTest extends GroovyTestCase {
    public void testDoInParralelWithException() {
        shouldFail {
            Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
                AsyncInvokerUtil.doInParallel({20}, {throw new RuntimeException('test1')}, {throw new RuntimeException('test2')}, {10})
            }
        }
    }

    public void testExecuteInParralelWithException() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            List<Future<Object>> result = AsyncInvokerUtil.executeInParallel({20}, {throw new RuntimeException('test1')}, {throw new RuntimeException('test2')}, {10})
            shouldFail {
                result*.get()
            }
        }
    }

    public void testStartInParralelWithException() {
        final AtomicReference<Throwable> thrownException = new AtomicReference<Throwable>()
        final CountDownLatch latch = new CountDownLatch(4)
        UncaughtExceptionHandler handler = {thread, throwable -> thrownException.set(throwable)} as UncaughtExceptionHandler

        Thread thread=AsyncInvokerUtil.startInParallel(
                handler,
                {latch.countDown()},
                {latch.countDown(); throw new RuntimeException('test1')},
                {latch.countDown(); throw new RuntimeException('test2')},
                {latch.countDown()})

        latch.await()
        thread.join()
        assert thrownException.get().cause instanceof AsyncException
    }

    public void testEachWithException() {
        shouldFail(AsyncException.class) {
            Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
                'abc'.eachAsync {throw new RuntimeException('test')}
            }
        }
    }

    public void testCollectWithException() {
        shouldFail(AsyncException.class) {
            Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
                'abc'.collectAsync {if (it=='b') throw new RuntimeException('test')}
            }
        }
    }

    public void testFindAllWithException() {
        shouldFail(AsyncException.class) {
            Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
                'abc'.findAllAsync {if (it=='b') throw new RuntimeException('test') else return true}
            }
        }
    }

    public void testFindWithException() {
        shouldFail(AsyncException.class) {
            Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
                'abc'.findAsync {if (it=='b') throw new RuntimeException('test') else return true}
            }
        }
    }
    
    public void testAllWithException() {
        shouldFail(AsyncException.class) {
            Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
                'abc'.allAsync {if (it=='b') throw new RuntimeException('test')}
            }
        }
    }

    public void testAnyWithException() {
        shouldFail(AsyncException.class) {
            Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
                'abc'.anyAsync {if (it=='b') throw new RuntimeException('test')}
            }
        }
    }
}