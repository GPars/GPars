package org.gparallelizer

import java.util.concurrent.ConcurrentSkipListSet

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ParallelizerTest extends GroovyTestCase {
    public void testEachAsyncWithThreadPool() {
        Parallelizer.withParallelizer(5) {
            def result = new ConcurrentSkipListSet()
            [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testEachAsyncOnEmpty() {
        Parallelizer.withParallelizer(5) {
            [].eachAsync{throw new RuntimeException('Should not be thrown')}
            [].eachAsync{throw new RuntimeException('Should not be thrown')}
            [].eachAsync{throw new RuntimeException('Should not be thrown')}
            ''.eachAsync{throw new RuntimeException('Should not be thrown')}
            [].iterator().eachAsync{throw new RuntimeException('Should not be thrown')}
            ''.iterator().eachAsync{throw new RuntimeException('Should not be thrown')}
        }
    }

    public void testCollectAsyncWithThreadPool() {
        Parallelizer.withParallelizer(5) {
            def result = [1, 2, 3, 4, 5].collectAsync {Number number -> number * 10}
            assertEquals([10, 20, 30, 40, 50], result)
        }
    }

    public void testFindAllAsyncWithThreadPool() {
        Parallelizer.withParallelizer(5) {
            def result = [1, 2, 3, 4, 5].findAllAsync {Number number -> number > 3}
            assertEquals([4, 5], result)
        }
    }

    public void testFindAsyncWithThreadPool() {
        Parallelizer.withParallelizer(5) {
            def result = [1, 2, 3, 4, 5].findAsync {Number number -> number > 3}
            assert (result in [4, 5])
        }
    }

    public void testAnyAsyncWithThreadPool() {
        Parallelizer.withParallelizer(5) {
            assert [1, 2, 3, 4, 5].anyAsync {Number number -> number > 3}
            assert ![1, 2, 3].anyAsync {Number number -> number > 3}
        }
    }

    public void testAllAsyncWithThreadPool() {
        Parallelizer.withParallelizer(5) {
            assert ![1, 2, 3, 4, 5].allAsync {Number number -> number > 3}
            assert [1, 2, 3].allAsync() {Number number -> number <= 3}
        }
    }

    public void testCategoryUsage() {
        Parallelizer.withParallelizer(5) {
            assertEquals(new HashSet([2, 4, 6]), new HashSet((Collection)new HashSet([1, 2, 3]).collectAsync {it * 2}))
            assertEquals(new HashSet([2, 3]), new HashSet((Collection)[1, 2, 3].findAllAsync {it > 1}))
        }
    }
}