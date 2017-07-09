// GPars - Groovy Parallel Systems
//
// Copyright © 2008--2011  The original author or authors
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
@SuppressWarnings("SpellCheckingInspection")
public class GParsPoolUtilTest extends GroovyTestCase {

    public void testEach() {
        groovyx.gpars.GParsPool.withPool(5) {
            final AtomicInteger result = new AtomicInteger(0)
            GParsPoolUtil.eachParallel([1, 2, 3, 4, 5], { result.addAndGet(it) })
            assert 15 == result

            final Set<Long> threadsIds = new ConcurrentSkipListSet<>()
            final CyclicBarrier barrier = new CyclicBarrier(5)
            GParsPoolUtil.eachParallel(0..4, {
                threadsIds.add(Thread.currentThread().getId())
                barrier.await()
            })
            assert 5 == threadsIds.size()


            final AtomicInteger mapResult = new AtomicInteger(0);
            GParsPoolUtil.eachParallel([1: 2, 3: 4, 5: 6], { mapResult.addAndGet(it.value) })
            assert 12 == mapResult
        }
    }

    public void testEachWithIndex() {
        groovyx.gpars.GParsPool.withPool(5) {
            final AtomicInteger result = new AtomicInteger(0)
            GParsPoolUtil.eachWithIndexParallel([1, 2, 3, 4, 5], { element, int index -> result.addAndGet(element * index) })
            assert 40 == result

            final Map<String, Integer> lettersMap = new ConcurrentHashMap<>();
            GParsPoolUtil.eachWithIndexParallel('abcde', { el, int index -> lettersMap.put(el, index) })
            assert 0 == lettersMap.get("a")
            assert 1 == lettersMap.get("b")
            assert 2 == lettersMap.get("c")
            assert 3 == lettersMap.get("d")
            assert 4 == lettersMap.get("e")

            final ConcurrentMap<Integer, Integer> map = new ConcurrentHashMap<>()
            GParsPoolUtil.eachWithIndexParallel([1: 1, 2: 2, 3: 3, 4: 4], { el, int index -> map.put(el.key, el.value * index) })
            assert [1: 0, 2: 2, 3: 6, 4: 12] == map
        }
    }

    public void testCollect() {
        groovyx.gpars.GParsPool.withPool(5) {
            final List result = GParsPoolUtil.collectParallel([1, 2, 3, 4, 5], { it * 2 })
            assert ([2, 4, 6, 8, 10] == result)
            assert ([2, 41, 6, 8, 10] != result)

            assert ['aa', 'bb', 'cc', 'dd', 'ee'] == GParsPoolUtil.collectParallel('abcde', { it * 2 })
        }
    }

    public void testCollectMany() {
        groovyx.gpars.GParsPool.withPool(5) {
            def squaresAndCubesOfOdds = [1, 2, 3, 4, 5].collectManyParallel { Number number ->
                number % 2 ? [number**2, number**3] : []
            }
            assert squaresAndCubesOfOdds == [1, 1, 9, 27, 25, 125]
        }
    }

    public void testFindAll() {
        groovyx.gpars.GParsPool.withPool(5) {
            final List result = GParsPoolUtil.findAllParallel([1, 2, 3, 4, 5], { it > 2 })
            assert !(1 in result)
            assert !(1 in result)
            assert (3 in result)
            assert (4 in result)
            assert (5 in result)
            assert 3 == result.size()

            assert [1: 'foo', 3: 'foo'] == GParsPoolUtil.findAllParallel([1: 'foo', 2: 'bar', 3: 'foo'], { it.value == 'foo'})
        }
    }

    public void testSplit() {
        groovyx.gpars.GParsPool.withPool(5) {
            def result = [1, 2, 3, 4, 5].splitParallel { it > 2 }
            assert [3, 4, 5] as Set == result[0] as Set
            assert [1, 2] as Set == result[1] as Set
            assert 2 == result.size()
            assert [[], []] == [].splitParallel { it > 2 }
            result = [3].splitParallel { it > 2 }
            assert [[3], []] == result
            result = [1].splitParallel { it > 2 }
            assert [[], [1]] == result
        }
    }

    public void testSplitOnString() {
        groovyx.gpars.GParsPool.withPool(5) {
            def result = 'abc'.splitParallel { it == 'b' }
            assert ['b'] as Set == result[0] as Set
            assert ['a', 'c'] as Set == result[1] as Set
            assert 2 == result.size()
            result = ''.splitParallel { it == 'b' }
            assert [[], []] == result
            result = 'b'.splitParallel { it == 'b' }
            assert [['b'], []] == result
            result = 'a'.splitParallel { it == 'b' }
            assert [[], ['a']] == result
        }
    }

    public void testGrep() {
        groovyx.gpars.GParsPool.withPool(5) {
            final List result = GParsPoolUtil.grepParallel([1, 2, 3, 4, 5], 3..6)
            assert !(1 in result)
            assert !(1 in result)
            assert (3 in result)
            assert (4 in result)
            assert (5 in result)
            assert 3 == result.size()

            assert ['o','r', 'e', 'm', 'm', 'o', 'l', 'o', 'r', 'm', 'e'] == GParsPoolUtil.grepParallel('Lorem ipsum dolor sit amet', 'lorem')

            assert [1: 'foo'] == GParsPoolUtil.grepParallel([0: 0, 1: 'foo', 2: 'bar'], [1: 'foo', 2: 2, 3: 'bar'])
        }
    }

    public void testEmptyFindAll() {
        groovyx.gpars.GParsPool.withPool(5) {
            final List<Integer> result = GParsPoolUtil.findAllParallel([1, 2, 3, 4, 5], { it > 6 })
            assert 0 == result.size()
        }
    }

    public void testFind() {
        groovyx.gpars.GParsPool.withPool(5) {
            final def result = GParsPoolUtil.findParallel([1, 2, 3, 4, 5], { it > 2 })
            assert result in [3, 4, 5]
            assert 3 == result

            assert 1 == GParsPoolUtil.findParallel([1: 'foo', 2: 'bar', 3: 'foo'], { it.value == 'foo' }).getKey()
        }
    }

    public void testEmptyFind() {
        groovyx.gpars.GParsPool.withPool(5) {
            final def result = GParsPoolUtil.findParallel([1, 2, 3, 4, 5], { it > 6 })
            assertNull result
        }
    }

    public void testFindAny() {
        groovyx.gpars.GParsPool.withPool(5) {
            final int result = GParsPoolUtil.findAnyParallel([1, 2, 3, 4, 5], { it > 2 })
            assert result in [3, 4, 5]

            assert GParsPoolUtil.findAnyParallel([1: 'foo', 2: 'bar', 3: 'foo'], { it.value == 'foo' }).getKey() in [1,3]
        }
    }

    public void testLazyFindAny() {
        groovyx.gpars.GParsPool.withPool(2) {
            final AtomicInteger counter = new AtomicInteger(0)
            final int result = GParsPoolUtil.findAnyParallel([1, 2, 3, 4, 5], { counter.incrementAndGet(); it > 0 })
            assert result in [1, 2, 3, 4, 5]
            assert counter.get() <= 2
        }
    }

    public void testEmptyFindAny() {
        groovyx.gpars.GParsPool.withPool(5) {
            final Integer result = GParsPoolUtil.findAnyParallel([1, 2, 3, 4, 5], { it > 6 })
            assertNull result
        }
    }

    public void testAny() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert GParsPoolUtil.anyParallel([1, 2, 3, 4, 5], { it > 2 })
            assert !GParsPoolUtil.anyParallel([1, 2, 3, 4, 5], { it > 6 })
            assert !GParsPoolUtil.anyParallel([1: 2, 3: 4, 5: 6], { it.key % 2 == 0 })
            assert GParsPoolUtil.anyParallel([1: 2, 3: 4, 5: 6], { it.value % 2 == 0 })
        }
    }

    public void testLazyAny() {
        def counter = new AtomicInteger(0)
        groovyx.gpars.GParsPool.withPool(2) {
            assert GParsPoolUtil.anyParallel([1, 2, 3, 4, 5], { counter.incrementAndGet(); it > 0 })
        }
        assert counter.get() <= 2
    }

    public void testAll() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert GParsPoolUtil.everyParallel([1, 2, 3, 4, 5], { it > 0 })
            assert !GParsPoolUtil.everyParallel([1, 2, 3, 4, 5], { it > 1 })

            assert GParsPoolUtil.everyParallel('eye', { it in 'aeuioy'.split('') })
            assert !GParsPoolUtil.everyParallel('eye', { it in 'qwert'.split('') })

            assert GParsPoolUtil.everyParallel([0: 20, 1: 23, 2: 42, 3: 213], { it.value > 10 })
            assert !GParsPoolUtil.everyParallel([0: 20, 1: 23, 2: 42, 3: 213], { it.key > 2 })

        }
    }

    @SuppressWarnings("GroovyOverlyComplexBooleanExpression")
    public void testGroupBy() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert GParsPoolUtil.groupByParallel([1, 2, 3, 4, 5], { it > 2 })
            assert GParsPoolUtil.groupByParallel([1, 2, 3, 4, 5], { Number number -> 1 }).size() == 1
            assert GParsPoolUtil.groupByParallel([1, 2, 3, 4, 5], { Number number -> number }).size() == 5
            final def groups = GParsPoolUtil.groupByParallel([1, 2, 3, 4, 5], { Number number -> number % 2 })
            assert groups.size() == 2
            assert (groups[0].containsAll([2, 4]) && groups[0].size() == 2) || (groups[0].containsAll([1, 3, 5]) && groups[0].size() == 3)
            assert (groups[1].containsAll([2, 4]) && groups[1].size() == 2) || (groups[1].containsAll([1, 3, 5]) && groups[1].size() == 3)

        }
    }

    public void testParallelPools() {
        final AtomicReference reference = new AtomicReference()
        final CyclicBarrier barrier1 = new CyclicBarrier(2)
        final CyclicBarrier barrier2 = new CyclicBarrier(2)

        def thread = new Thread({
            groovyx.gpars.GParsPool.withPool(2) {
                final ForkJoinPool pool2 = groovyx.gpars.GParsPool.retrieveCurrentPool()
                reference.set pool2
                barrier1.await()
                barrier2.await()
            }
        } as Runnable)
        thread.start()
        groovyx.gpars.GParsPool.withPool(5) {
            final ForkJoinPool pool1 = groovyx.gpars.GParsPool.retrieveCurrentPool()
            barrier1.await()
            final ForkJoinPool nestedPool = reference.get() as ForkJoinPool
            assert pool1 != nestedPool
            barrier2.await()
        }
    }

    public void testMissingPool() {
        final AtomicInteger counter = new AtomicInteger(0)
        shouldFail(IllegalStateException.class) {
            GParsPoolUtil.eachParallel([], { counter.incrementAndGet() })
        }
        assert 0 == counter.get()
    }

    public void testNestedPools() {
        groovyx.gpars.GParsPool.withPool { a ->
            groovyx.gpars.GParsPool.withPool { b ->
                groovyx.gpars.GParsPool.withPool { c ->
                    groovyx.gpars.GParsPool.withPool { d ->
                        assert d != c != b != a
                        assert groovyx.gpars.GParsPool.retrieveCurrentPool() == d
                    }
                    assert groovyx.gpars.GParsPool.retrieveCurrentPool() == c
                }
                assert groovyx.gpars.GParsPool.retrieveCurrentPool() == b
            }
            assert groovyx.gpars.GParsPool.retrieveCurrentPool() == a
        }
    }

    public void testNestedExistingPools() {
        final def pool1 = new ForkJoinPool()
        final def pool2 = new ForkJoinPool()
        final def pool3 = new ForkJoinPool()
        groovyx.gpars.GParsPool.withExistingPool(pool1) { a ->
            groovyx.gpars.GParsPool.withExistingPool(pool2) { b ->
                groovyx.gpars.GParsPool.withExistingPool(pool1) { c ->
                    groovyx.gpars.GParsPool.withExistingPool(pool3) { d ->
                        assert d == pool3
                        assert c == pool1
                        assert b == pool2
                        assert a == pool1
                        assert groovyx.gpars.GParsPool.retrieveCurrentPool() == pool3
                    }
                    assert groovyx.gpars.GParsPool.retrieveCurrentPool() == pool1
                }
                assert groovyx.gpars.GParsPool.retrieveCurrentPool() == pool2
            }
            assert groovyx.gpars.GParsPool.retrieveCurrentPool() == pool1
        }
        pool1.shutdown()
        pool2.shutdown()
        pool3.shutdown()
    }

    public void testExceptionHandler() {
        final AtomicInteger exceptionCount = new AtomicInteger(0)
        def handler = { Thread t, Throwable e ->
            exceptionCount.incrementAndGet()
        } as UncaughtExceptionHandler

        shouldFail(RuntimeException.class) {
            groovyx.gpars.GParsPool.withPool(5, handler) { ForkJoinPool pool ->
                'abc'.eachParallel { throw new RuntimeException('test') }
            }
        }
//        assertEquals 3, exceptionCount.get()
    }

    public testNestedCalls() {
        groovyx.gpars.GParsPool.withPool(5) { pool ->
            def result = ['abc', '123', 'xyz'].findAllParallel { word ->
                groovyx.gpars.GParsPool.withExistingPool(pool) {
                    word.anyParallel { it in ['a', 'y', '5'] }
                }
            }
            assert ['abc', 'xyz'] == result
        }
    }

    public void testMin() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert 1 == [1, 2, 3, 4, 5].minParallel { a, b -> a - b }
            assert 5 == [1, 2, 3, 4, 5].minParallel { a, b -> b - a }
            assert 1 == [1, 2, 3, 4, 5].minParallel { it }
            assert 1 == [1, 2, 3, 4, 5].minParallel { it * 2 }
            assert 1 == [1, 2, 3, 4, 5].minParallel { a -> a + 10 }
            assert 1 == [1, 2, 3, 4, 5].minParallel()
            assert 'a' == 'abc'.minParallel()
            assert 'a' == 'cba'.minParallel()
        }
    }

    public void testMax() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert 5 == [1, 2, 3, 4, 5].maxParallel { a, b -> a - b }
            assert 1 == [1, 2, 3, 4, 5].maxParallel { a, b -> b - a }
            assert 5 == [1, 2, 3, 4, 5].maxParallel { it }
            assert 5 == [1, 2, 3, 4, 5].maxParallel { it * 2 }
            assert 5 == [1, 2, 3, 4, 5].maxParallel { a -> a + 10 }
            assert 5 == [1, 2, 3, 4, 5].maxParallel()
            assert 'c' == 'abc'.maxParallel()
            assert 'c' == 'cba'.maxParallel()
        }
    }

    public void testSum() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert 15 == [1, 2, 3, 4, 5].sumParallel()
            assert 'abc' == 'abc'.sumParallel()
        }
    }

    public void testCount() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert 1 == [1, 2, 3, 4, 5].countParallel(3)
            assert 5 == [3, 2, 3, 4, 5, 3, 3, 3].countParallel(3)
            assert 0 == [3, 2, 3, 4, 5, 3, 3, 3].countParallel(6)
            assert 2 == [3, 2, 3, 4, 5, 3, 3, 3].countParallel { it % 2 == 0 }
            assert 1 == [3, 2, 3, 4, 5, 3, 3, 3].countParallel { it < 3 }
            assert 6 == [3, 2, 3, 4, 5, 3, 3, 3].countParallel { it <= 3 }
            assert 0 == [].countParallel(6)
            assert 1 == 'abc'.countParallel('a')
            assert 3 == 'abcaa'.countParallel('a')
            assert 0 == 'ebc'.countParallel('a')
            assert 0 == ''.countParallel('a')
            assert 3 == 'elephant'.countParallel { it in 'aeiou'.toList() }
        }
    }

    public void testReduce() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert 15 == [1, 2, 3, 4, 5].injectParallel() { a, b -> a + b }
            assert 'abc' == 'abc'.injectParallel { a, b -> a + b }
            assert 55 == [1, 2, 3, 4, 5].collectParallel { it**2 }.injectParallel { a, b -> a + b }
        }
    }

    public void testSeededReduce() {
        groovyx.gpars.GParsPool.withPool(1) {
            assert 15 == [1, 2, 3, 4, 5].injectParallel(0) { a, b -> a + b }
            assert 25 == [1, 2, 3, 4, 5].injectParallel(10) { a, b -> a + b }
            assert 1200 == [1, 2, 3, 4, 5].injectParallel(10) { a, b -> a * b }
            assert 'abc' == 'abc'.injectParallel('') { a, b -> a + b }
            assert 'abcd' == 'abc'.injectParallel('d') { a, b -> a + b }
        }
    }

    public void testReduceThreads() {
        final Map map = new ConcurrentHashMap()

        groovyx.gpars.GParsPool.withPool(5) {
            assert 55 == [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].injectParallel { a, b ->
                Thread.sleep 200
                map[Thread.currentThread()] = ''
                a + b
            }
            assert map.keys().size() > 1
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
        groovyx.gpars.GParsPool.withPool {
            methods.each { method, expected ->
                assert expected == x."${method}Parallel"({
                    it % 2
                }): "Surprise when processing parallel version of $method"
            }
        }
    }

    public void testNonBooleanParallelFindAny() {
        def x = [1, 2, 3]
        groovyx.gpars.GParsPool.withPool {
            // Really just making sure it doesn't explode, but what the Hell...
            assert "Surprise when processing parallel version of find", x.findAnyParallel({ it % 2 }) in [1, 3]
        }
    }
}
