package org.gparallelizer

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ParallelizerStringTest extends GroovyTestCase {
    public void testEachAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
          def result = Collections.synchronizedSet(new HashSet())
            'abc'.eachAsync {result.add(it.toUpperCase())}
            assertEquals(new HashSet(['A', 'B', 'C']), result)
        }
    }

    public void testCollectAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'abc'.collectAsync {it.toUpperCase()}
            assertEquals(['A', 'B', 'C'], result)
        }
    }

    public void testFindAllAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'aBC'.findAllAsync {it == it.toUpperCase()}
            assertEquals(['B', 'C'], result)
        }
    }

    public void testFindAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'aBC'.findAsync {it == it.toUpperCase()}
            assert (result in ['B', 'C'])
        }
    }

    public void testAnyAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            assert 'aBc'.anyAsync {it == it.toUpperCase()}
            assert !'abc'.anyAsync {it == it.toUpperCase()}
        }
    }

    public void testAllAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            assert !'aBC'.allAsync {it == it.toUpperCase()}
            assert 'ABC'.allAsync() {it == it.toUpperCase()}
        }
    }
}
