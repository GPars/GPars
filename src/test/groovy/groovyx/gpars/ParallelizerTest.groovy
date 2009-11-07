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

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ParallelizerTest extends GroovyTestCase {
    public void testEachParallelWithThreadPool() {
        Parallelizer.doParallel(5) {
            def result = Collections.synchronizedSet(new HashSet())
            [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testEachParallelOnOneElementCollections() {
        Parallelizer.doParallel(5) {
            [1].eachParallel {}
            [1].eachParallel {}
            [1].eachParallel {}
            'a'.eachParallel {}
            [1].iterator().eachParallel {}
            'a'.iterator().eachParallel {}
        }
    }

    public void testEachParallelOnEmpty() {
        Parallelizer.doParallel(5) {
            [].eachParallel {throw new RuntimeException('Should not be thrown')}
            [].eachParallel {throw new RuntimeException('Should not be thrown')}
            [].eachParallel {throw new RuntimeException('Should not be thrown')}
            ''.eachParallel {throw new RuntimeException('Should not be thrown')}
            [].iterator().eachParallel {throw new RuntimeException('Should not be thrown')}
            ''.iterator().eachParallel {throw new RuntimeException('Should not be thrown')}
        }
    }

    public void testCollectParallelWithThreadPool() {
        Parallelizer.doParallel(5) {
            def result = [1, 2, 3, 4, 5].collectParallel {Number number -> number * 10}
            assertEquals([10, 20, 30, 40, 50], result)
        }
    }

    public void testCollectParallelWithThreadPoolOnRange() {
        Parallelizer.doParallel(5) {
            def result = (1..5).collectParallel {Number number -> number * 10}
            assertEquals([10, 20, 30, 40, 50], result)
        }
    }

    public void testFindAllParallelWithThreadPool() {
        Parallelizer.doParallel(5) {
            def result = [1, 2, 3, 4, 5].findAllParallel {Number number -> number > 3}
            assertEquals([4, 5], result)
        }
    }

    public void testFindParallelWithThreadPool() {
        Parallelizer.doParallel(5) {
            def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 3}
            assert (result in [4, 5])
        }
    }

    public void testAnyParallelWithThreadPool() {
        Parallelizer.doParallel(5) {
            assert [1, 2, 3, 4, 5].anyParallel {Number number -> number > 3}
            assert ![1, 2, 3].anyParallel {Number number -> number > 3}
        }
    }

    public void testAllParallelWithThreadPool() {
        Parallelizer.doParallel(5) {
            assert ![1, 2, 3, 4, 5].everyParallel {Number number -> number > 3}
            assert [1, 2, 3].everyParallel() {Number number -> number <= 3}
        }
    }

    @SuppressWarnings("GroovyOverlyComplexBooleanExpression")
    public void testGroupBy() {
        Parallelizer.withParallelizer(5) {
            assert [1, 2, 3, 4, 5].groupByParallel {it > 2}
            assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> 1}).size() == 1
            assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> number}).size() == 5
            final def groups = ([1, 2, 3, 4, 5].groupByParallel {Number number -> number % 2})
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
        Parallelizer.withParallelizer {
            assertEquals([0, 1, 2, 3], qsort([0, 3, 1, 2]))
        }
    }

    public void testCategoryUsage() {
        Parallelizer.doParallel(5) {
            assertEquals(new HashSet([2, 4, 6]), new HashSet((Collection) new HashSet([1, 2, 3]).collectParallel {it * 2}))
            assertEquals(new HashSet([2, 3]), new HashSet((Collection) [1, 2, 3].findAllParallel {it > 1}))
        }
    }
}
