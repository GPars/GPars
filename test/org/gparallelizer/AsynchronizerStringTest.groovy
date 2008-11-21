package org.gparallelizer

import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.ExecutorService

/**
 * @author Vaclav Pech
 * Date: Nov 10, 2008
 */
public class AsynchronizerStringTest extends GroovyTestCase {

    public void testEachAsyncWithString() {
        def result = new ConcurrentSkipListSet()
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            'abc'.eachAsync {result.add(it.toUpperCase())}
            assertEquals(new HashSet(['A', 'B', 'C']), result)
        }
    }

    public void testCollectAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = 'abc'.collectAsync{it.toUpperCase()}
            assertEquals(['A', 'B', 'C'], result)
        }
    }

    public void testFindAllAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = 'aBC'.findAllAsync{it == it.toUpperCase()}
            assertEquals(['B', 'C'], result)
        }
    }

    public void testFindAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = 'aBC'.findAsync{it == it.toUpperCase()}
            assert result in ['B', 'C']
        }
    }

    public void testAllAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert 'ABC'.allAsync{it == it.toUpperCase()}
            assert !'aBC'.allAsync{it == it.toUpperCase()}
        }
    }

    public void testAnyAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert 'aBc'.anyAsync{it == it.toUpperCase()}
            assert 'aBC'.anyAsync{it == it.toUpperCase()}
            assert !'abc'.anyAsync{it == it.toUpperCase()}
        }
    }
}