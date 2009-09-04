package org.gparallelizer

public class AsynchronizerIteratorTest extends GroovyTestCase {
    public void testIteratorEach() {
        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

        Asynchronizer.withAsynchronizer {
          def result = Collections.synchronizedSet(new HashSet())
            list.iterator().eachAsync {
                result << it
            }
            assertEquals 9, result.size()
        }
    }

    public void testIteratorCollect() {
        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

        Asynchronizer.withAsynchronizer {
            def result = list.iterator().collectAsync { 2*it }
            assertEquals 9, result.size()
            assert result.any {it == 12}
        }
    }

    public void testIterator() {
        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

        Asynchronizer.withAsynchronizer {
            assert list.iterator().anyAsync { it == 6 }
            assert list.iterator().allAsync { it < 10 }
            assertEquals 8, list.iterator().findAsync { it == 8 }
            assertEquals 3, (list.iterator().findAllAsync { it > 6 }).size()
        }
    }
}
