// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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
class MakeTransparentTest extends GroovyTestCase {

    public void testMakeTransparentAvailability() {
        shouldFail {
            [].makeTransparent()
            [1].makeTransparent()
            'abcde'.makeTransparent()
        }

        def items = [1, 2, 3, 4, 5]

        shouldFail {
            items.makeTransparent()
        }

        GParsPool.withPool {
            assertNotNull([1].makeTransparent())
            assertNotNull('abcde'.makeTransparent())
            assertEquals items, items.makeTransparent()
            assertNotNull(items.makeTransparent())
            assert items.makeTransparent() == items.makeTransparent().makeTransparent()
            final def p1 = items.makeTransparent()
            assert p1 == p1.makeTransparent()
        }

        shouldFail {
            [1].makeTransparent()
        }

        shouldFail {
            items.makeTransparent()
        }
    }

    public void testMakeTransparentTypeCompatibility() {
        GParsPool.withPool {
            Collection c = [1, 2, 3, 4, 5].makeTransparent()
            String s = 'abcde'.makeTransparent()
            assert !c.isEmpty()
            assert s.size() > 0
        }
    }

    public void testNonTransparentAfterClone() {
        GParsPool.withPool {
            Collection c = [1, 2, 3, 4, 5].makeTransparent()
            assert c.isConcurrent()
            assertFalse c.clone().isConcurrent()
        }
    }

    public void testIsTransparentCheck() {
        def items = [1, 2, 3, 4, 5]
        shouldFail {
            items.isConcurrent()
        }
        shouldFail(IllegalStateException) {
            GParsPoolUtil.makeTransparent(items)
        }
        GParsPool.withPool {
            assertFalse items.isConcurrent()
            assertFalse 'abc'.isConcurrent()
            assertTrue items.makeTransparent().isConcurrent()
            assertTrue items.isConcurrent()
            assertTrue 'abcde'.makeTransparent().isConcurrent()
        }

        assertTrue items.isConcurrent()
        assertTrue 'abcde'.isConcurrent()
        shouldFail {
            assertTrue 'ab'.isConcurrent()
        }

        shouldFail(IllegalStateException) {
            GParsPoolUtil.makeTransparent('abcdefgh2')
        }
    }

    public void testIdempotenceOfNestingMakeTransparent() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeTransparent().makeTransparent().each {
                barrier.await()
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() == 5
    }

    public void testMakeTransparentPropagationToResults() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeTransparent().collect {it * 2}.findAll {it > 1}.each {
                barrier.await()
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() == 5
    }

    public void testNoMakeTransparentPropagationToResultsWithGroupBy() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        GParsPool.withPool(5) {
            items.makeTransparent().groupBy {it % 2}.each {
                Thread.sleep 500
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() == 1
    }

    public void testMakeTransparentPropagationToResultsWithString() {
        def items = 'abcde'
        final Map map = new ConcurrentHashMap()

        GParsPool.withPool(5) {
            items.makeTransparent().collect {it * 2}.findAll {it.size() > 1}.each {
                sleep 500
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() > 1
    }

    public void testMakeTransparentPropagationToResultsWithIterator() {
        def items = [1, 2, 3, 4, 5].iterator()
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(5)

        GParsPool.withPool(5) {
            items.makeTransparent().collect {it * 2}.findAll {it > 1}.each {
                barrier.await()
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() == 5
    }

    public void testTransparentParallelInMethodCall() {
        def items = [1, 2, 3, 4, 5]
        assertEquals 1, foo(items, 1).keys().size()

        GParsPool.withPool(5) {
            assertEquals 1, foo(items, 1).keys().size()
            assert foo(items.makeTransparent(), 5).keys().size() == 5
        }
    }

    private def foo(Collection c, int count) {
        final Map map = new ConcurrentHashMap()
        final CyclicBarrier barrier = new CyclicBarrier(count)

        c.collect {it * 2}.findAll {it > 1}.each {
            barrier.await()
            map[Thread.currentThread()] = ''
        }
        return map
    }
}
