// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CyclicBarrier

/**
 * Author: Vaclav Pech
 * Date: Oct 30, 2009
 */

@SuppressWarnings("SpellCheckingInspection")
class MakeTransparentMethodTest extends GroovyTestCase {

    public void testTransparentEach() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeConcurrent().each {
                barrier.await()
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentEachWithIndex() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeConcurrent().eachWithIndex { e, i ->
                barrier.await()
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentCollect() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeConcurrent().collect {
                barrier.await()
                map[Thread.currentThread()] = ''
                return it
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentGrep() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeConcurrent().grep {
                barrier.await()
                map[Thread.currentThread()] = ''
                return true
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentFind() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeConcurrent().find {
                barrier.await()
                map[Thread.currentThread()] = ''
                return false
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentFindAny() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeConcurrent().findAny {
                barrier.await()
                map[Thread.currentThread()] = ''
                return false
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentFindAll() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeConcurrent().findAll {
                barrier.await()
                map[Thread.currentThread()] = ''
                return true
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentAll() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)
        GParsPool.withPool(5) {
            items.makeConcurrent().every {
                barrier.await()
                map[Thread.currentThread()] = ''
                return true
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentAny() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeConcurrent().any {
                barrier.await()
                map[Thread.currentThread()] = ''
                return false
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentAnyOnString() {
        def items = 'abcdefg'
        final Map map = new ConcurrentHashMap()
        GParsPool.withPool(5) {
            items.makeConcurrent().any {
                sleep 500
                map[Thread.currentThread()] = ''
                return false
            }
        }
        assert map.keys().size() > 1
    }

    public void testTransparentGroupBy() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeConcurrent().groupBy {
                barrier.await()
                map[Thread.currentThread()] = ''
                return it
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentMin() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        GParsPool.withPool(5) {
            items.makeConcurrent().min { a, b ->
                Thread.sleep 100
                map[Thread.currentThread()] = ''
                return a - b
            }
        }
        assert map.keys().size() > 1
    }

    public void testTransparentMax() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        GParsPool.withPool(5) {
            items.makeConcurrent().max { a, b ->
                Thread.sleep 100
                map[Thread.currentThread()] = ''
                return a - b
            }
        }
        assert map.keys().size() > 1
    }

    public void testTransparentSum() {
        def items = [1, 2, 3, 4, 5]
        GParsPool.withPool(5) {
            assert 15 == items.makeConcurrent().sum()
        }
    }

    public void testCount() {
        GParsPool.withPool(5) {
            assert 1 == [1, 2, 3, 4, 5].makeConcurrent().count(3)
            assert 5 == [3, 2, 3, 4, 5, 3, 3, 3].makeConcurrent().count(3)
            assert 0 == [3, 2, 3, 4, 5, 3, 3, 3].makeConcurrent().count(6)
            assert 0 == [].makeConcurrent().count(6)
            assert 1 == 'abc1'.makeConcurrent().count('a')
            assert 3 == 'abcaa1'.makeConcurrent().count('a')
            assert 0 == 'ebc1'.makeConcurrent().count('a')
            assert 0 == '  '.trim().makeConcurrent().count('a')
        }
    }

    public void testSplit() {
        GParsPool.withPool(5) {
            def result = [1, 2, 3, 4, 5].makeConcurrent().split { it > 2 }
            assert [3, 4, 5] as Set == result[0] as Set
            assert [1, 2] as Set == result[1] as Set
            assert 2 == result.size()
            assert [[], []] == [].makeConcurrent().split { it > 2 }
            result = [3].makeConcurrent().split { it > 2 }
            assert [[3], []] == result
            result = [1].makeConcurrent().split { it > 2 }
            assert [[], [1]] == result
        }
    }

    public void testSplitOnString() {
        GParsPool.withPool(5) {
            def result = new String('abc').makeConcurrent().split { it == 'b' }
            assert ['b'] as Set == result[0] as Set
            assert ['a', 'c'] as Set == result[1] as Set
            assert 2 == result.size()
            result = ''.makeConcurrent().split { it == 'b' }
            assert [[], []] == result
            result = 'b'.makeConcurrent().split { it == 'b' }
            assert [['b'], []] == result
            result = 'a'.makeConcurrent().split { it == 'b' }
            assert [[], ['a']] == result
        }
    }

    public void testTransparentReduce() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        GParsPool.withPool(5) {
            items.makeConcurrent().inject { a, b ->
                Thread.sleep 100
                map[Thread.currentThread()] = ''
                return a + b
            }
        }
        assert map.keys().size() > 1
    }

    public void testTransparentSeededReduce() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        GParsPool.withPool(5) {
            items.makeConcurrent().inject(10) { a, b ->
                Thread.sleep 100
                map[Thread.currentThread()] = ''
                return a + b
            }
        }
        assert map.keys().size() > 1
    }
}
