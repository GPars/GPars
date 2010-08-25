// GPars - Groovy Parallel Systems
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
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class GParsExecutorsPoolTest extends GroovyTestCase {

    public void testDoInParallel() {
        GParsExecutorsPool.withPool {
            assertEquals([10, 20], GParsExecutorsPool.executeAsyncAndWait({10}, {20}))
        }
    }

    public void testExecuteInParallel() {
        GParsExecutorsPool.withPool {
            assertEquals([10, 20], GParsExecutorsPool.executeAsync({10}, {20})*.get())
        }
    }

    public void testDoInParallelList() {
        GParsExecutorsPool.withPool {
            assertEquals([10, 20], GParsExecutorsPool.executeAsyncAndWait([{10}, {20}]))
        }
    }

    public void testExecuteAsyncList() {
        GParsExecutorsPool.withPool {
            assertEquals([10, 20], GParsExecutorsPool.executeAsync([{10}, {20}])*.get())
        }
    }

    public void testAsyncWithCollectionAndResult() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            Collection<Future> result = [1, 2, 3, 4, 5].collect({it * 10}.async())
            assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection) result*.get()))
        }
    }

    public void testEachParallel() {
        def result = Collections.synchronizedSet(new HashSet())
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testEachParallelOnSingleElementCollections() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            [1].eachParallel {}
            [1].eachParallel {}
            [1].eachParallel {}
            'a'.eachParallel {}
            [1].iterator().eachParallel {}
            'a'.iterator().eachParallel {}
        }
    }

    public void testEachParallelOnEmpty() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            [].eachParallel {throw new RuntimeException('Should not be thrown')}
            [].eachParallel {throw new RuntimeException('Should not be thrown')}
            [].eachParallel {throw new RuntimeException('Should not be thrown')}
            ''.eachParallel {throw new RuntimeException('Should not be thrown')}
            [].iterator().eachParallel {throw new RuntimeException('Should not be thrown')}
            ''.iterator().eachParallel {throw new RuntimeException('Should not be thrown')}
        }
    }

    public void testEachWithIndexParallel() {
        def result = Collections.synchronizedSet(new HashSet())
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            [1, 2, 3, 4, 5].eachWithIndexParallel {Number number, int index -> result.add(number * index)}
            assertEquals(new HashSet([0, 2, 6, 12, 20]), result)
        }
    }

    public void testEachWithIndexParallelOnSingleElementCollections() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            [1].eachWithIndexParallel {e, i ->}
            [1].eachWithIndexParallel {e, i ->}
            [1].eachWithIndexParallel {e, i ->}
            'a'.eachWithIndexParallel {e, i ->}
            [1].iterator().eachWithIndexParallel {e, i ->}
            'a'.iterator().eachWithIndexParallel {e, i ->}
        }
    }

    public void testEachWithIndexParallelOnEmpty() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            [].eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            [].eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            [].eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            ''.eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            [].iterator().eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            ''.iterator().eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
        }
    }

    public void testCollectParallel() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].collectParallel {Number number -> number * 10}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection) result))
        }
    }

    public void testCollectParallelOnRange() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            def result = (1..5).collectParallel {Number number -> number * 10}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection) result))
        }
    }

    public void testFindAllParallel() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].findAllParallel {Number number -> number > 2}
            assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection) result))
        }
    }

    public void testGrepParallel() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].grepParallel(3..6)
            assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection) result))
        }
    }

    public void testFindParallel() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 2}
            assert result in [3, 4, 5]
            assertEquals 3, result
        }
    }

    public void testFindAnyParallel() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].findAnyParallel {Number number -> number > 2}
            assert result in [3, 4, 5]
        }
    }

    public void testLazyFindAnyParallel() {
        GParsExecutorsPool.withPool(2) {ExecutorService service ->
            final AtomicInteger counter = new AtomicInteger(0)
            def result = [1, 2, 3, 4, 5].findAnyParallel {Number number -> counter.incrementAndGet(); number > 0}
            assert result in [1, 2, 3, 4, 5]
            assert counter.get() <= 2
        }
    }

    public void testAllParallel() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            assert [1, 2, 3, 4, 5].everyParallel {Number number -> number > 0}
            assert ![1, 2, 3, 4, 5].everyParallel {Number number -> number > 2}
        }
    }

    public void testAnyParallel() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            assert [1, 2, 3, 4, 5].anyParallel {Number number -> number > 0}
            assert [1, 2, 3, 4, 5].anyParallel {Number number -> number > 2}
            assert ![1, 2, 3, 4, 5].anyParallel {Number number -> number > 6}
        }
    }

    public void testLazyAnyParallel() {
        GParsExecutorsPool.withPool(2) {ExecutorService service ->
            def counter = new AtomicInteger(0)
            assert [1, 2, 3, 4, 5].anyParallel {Number number -> counter.incrementAndGet(); number > 0}
            assert counter.get() <= 2
        }
    }

    @SuppressWarnings("GroovyOverlyComplexBooleanExpression")
    public void testGroupByParallel() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
            assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> 1}).size() == 1
            assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> number}).size() == 5
            final def groups = [1, 2, 3, 4, 5].groupByParallel {Number number -> number % 2}
            assert groups.size() == 2
            assert (groups[0].containsAll([2, 4]) && groups[0].size() == 2) || (groups[0].containsAll([1, 3, 5]) && groups[0].size() == 3)
            assert (groups[1].containsAll([2, 4]) && groups[1].size() == 2) || (groups[1].containsAll([1, 3, 5]) && groups[1].size() == 3)
        }
    }

    private def qsort(list) {
        if (!list) return []
        def bucket = list.groupByParallel { it <=> list.first() }
        [* qsort(bucket[-1]), * bucket[0], * qsort(bucket[1])]
    }

    public void testQuicksort() {
        GParsExecutorsPool.withPool {
            assertEquals([0, 1, 2, 3], qsort([0, 3, 1, 2]))
        }
    }

    public void testAsyncTask() {
        GParsExecutorsPool.withPool(5) {ExecutorService service ->
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

    public void testNonBooleanParallelMethods() {
        def methods = [
                "findAll": [1, 3],
                "find": 1,
                "any": true,
                "every": false
        ]
        def x = [1, 2, 3]
        GParsExecutorsPool.withPool {
            methods.each {method, expected ->
                // Really just making sure it doesn't explode, but what the Hell...
                assertEquals "Surprise when processing parallel version of $method", expected, x."${method}Parallel"({ it % 2 })
            }
        }
    }

    public void testNonBooleanParallelFindAny() {
        def x = [1, 2, 3]
        GParsExecutorsPool.withPool {
            // Really just making sure it doesn't explode, but what the Hell...
            assert "Surprise when processing parallel version of find", x.findAnyParallel({ it % 2 }) in [1, 3]
        }
    }
}
