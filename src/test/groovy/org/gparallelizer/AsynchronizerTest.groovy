package org.gparallelizer

import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class AsynchronizerTest extends GroovyTestCase {
    public void testStartInParallel() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def resultA=0, resultB=0
            final CountDownLatch latch = new CountDownLatch(2)
            AsyncInvokerUtil.startInParallel({resultA=1;latch.countDown()}, {resultB=1;latch.countDown()})
            latch.await()
            assertEquals 1, resultA
            assertEquals 1, resultB
        }
    }

    public void testDoInParallel() {
        assertEquals([10, 20], AsyncInvokerUtil.doInParallel({10}, {20}))
    }

    public void testExecuteInParallel() {
        assertEquals([10, 20], AsyncInvokerUtil.executeInParallel({10}, {20})*.get())
    }

    public void testAsyncWithCollectionAndResult() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            Collection<Future> result = [1, 2, 3, 4, 5].collect({it * 10}.async())
            assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result*.get()))
        }
    }

    public void testEachAsync() {
        def result = new ConcurrentSkipListSet()
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            [1, 2, 3, 4, 5].eachAsync{Number number -> result.add(number * 10)}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testEachAsyncOnsingleElementCollections() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            [1].eachAsync{}
            [1].eachAsync{}
            [1].eachAsync{}
            'a'.eachAsync{}
            [1].iterator().eachAsync{}
            'a'.iterator().eachAsync{}
        }
    }

    public void testEachAsyncOnEmpty() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            [].eachAsync{throw new RuntimeException('Should not be thrown')}
            [].eachAsync{throw new RuntimeException('Should not be thrown')}
            [].eachAsync{throw new RuntimeException('Should not be thrown')}
            ''.eachAsync{throw new RuntimeException('Should not be thrown')}
            [].iterator().eachAsync{throw new RuntimeException('Should not be thrown')}
            ''.iterator().eachAsync{throw new RuntimeException('Should not be thrown')}
        }
    }

    public void testCollectAsync() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].collectAsync{Number number -> number * 10}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result))
        }
    }

    public void testFindAllAsync() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].findAllAsync{Number number -> number > 2}
            assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection)result))
        }
    }

    public void testFindAsync() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].findAsync{Number number -> number > 2}
            assert result in [3, 4, 5]
        }
    }

    public void testAllAsync() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert [1, 2, 3, 4, 5].allAsync{Number number -> number > 0}
            assert ![1, 2, 3, 4, 5].allAsync{Number number -> number > 2}
        }
    }

    public void testAnyAsync() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert [1, 2, 3, 4, 5].anyAsync{Number number -> number > 0}
            assert [1, 2, 3, 4, 5].anyAsync{Number number -> number > 2}
            assert ![1, 2, 3, 4, 5].anyAsync{Number number -> number > 6}
        }
    }

    public void testAsyncTask() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            final AtomicBoolean flag = new AtomicBoolean(false)
            final CountDownLatch latch = new CountDownLatch(1)

            service.submit({
                flag.set(true)
                latch.countDown()
            } as Runnable)

            latch.await()
            assert flag.get()
        }
    }
}