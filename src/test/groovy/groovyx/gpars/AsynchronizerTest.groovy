//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package groovyx.gpars

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
            def resultA = 0, resultB = 0
            final CountDownLatch latch = new CountDownLatch(2)
            Asynchronizer.startInParallel({resultA = 1; latch.countDown()}, {resultB = 1; latch.countDown()})
            latch.await()
            assertEquals 1, resultA
            assertEquals 1, resultB
        }
    }

    public void testDoInParallel() {
        assertEquals([10, 20], Asynchronizer.doInParallel({10}, {20}))
    }

    public void testExecuteInParallel() {
        assertEquals([10, 20], Asynchronizer.executeAsync({10}, {20})*.get())
    }

    public void testAsyncWithCollectionAndResult() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            Collection<Future> result = [1, 2, 3, 4, 5].collect({it * 10}.async())
            assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection) result*.get()))
        }
    }

    public void testEachParallel() {
        def result = Collections.synchronizedSet(new HashSet())
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testEachParallelOnSingleElementCollections() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            [1].eachParallel {}
            [1].eachParallel {}
            [1].eachParallel {}
            'a'.eachParallel {}
            [1].iterator().eachParallel {}
            'a'.iterator().eachParallel {}
        }
    }

    public void testEachParallelOnEmpty() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
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
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            [1, 2, 3, 4, 5].eachWithIndexParallel {Number number, int index -> result.add(number * index)}
            assertEquals(new HashSet([0, 2, 6, 12, 20]), result)
        }
    }

    public void testEachWithIndexParallelOnsingleElementCollections() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            [1].eachWithIndexParallel {e, i ->}
            [1].eachWithIndexParallel {e, i ->}
            [1].eachWithIndexParallel {e, i ->}
            'a'.eachWithIndexParallel {e, i ->}
            [1].iterator().eachWithIndexParallel {e, i ->}
            'a'.iterator().eachWithIndexParallel {e, i ->}
        }
    }

    public void testEachWithIndexParallelOnEmpty() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            [].eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            [].eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            [].eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            ''.eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            [].iterator().eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
            ''.iterator().eachWithIndexParallel {e, i -> throw new RuntimeException('Should not be thrown')}
        }
    }

    public void testCollectParallel() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].collectParallel {Number number -> number * 10}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection) result))
        }
    }

    public void testCollectParallelOnRange() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = (1..5).collectParallel {Number number -> number * 10}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection) result))
        }
    }

    public void testFindAllParallel() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].findAllParallel {Number number -> number > 2}
            assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection) result))
        }
    }

    public void testGrepParallel() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].grepParallel(3..6)
            assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection) result))
        }
    }

    public void testFindParallel() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 2}
            assert result in [3, 4, 5]
        }
    }

    public void testAllParallel() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert [1, 2, 3, 4, 5].everyParallel {Number number -> number > 0}
            assert ![1, 2, 3, 4, 5].everyParallel {Number number -> number > 2}
        }
    }

    public void testAnyParallel() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert [1, 2, 3, 4, 5].anyParallel {Number number -> number > 0}
            assert [1, 2, 3, 4, 5].anyParallel {Number number -> number > 2}
            assert ![1, 2, 3, 4, 5].anyParallel {Number number -> number > 6}
        }
    }

    @SuppressWarnings("GroovyOverlyComplexBooleanExpression")
    public void testGroupByParallel() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
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
        Asynchronizer.withAsynchronizer {
            assertEquals([0, 1, 2, 3], qsort([0, 3, 1, 2]))
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
