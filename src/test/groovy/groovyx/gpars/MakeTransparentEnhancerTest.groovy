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

/**
 * Author: Vaclav Pech
 * Date: Oct 30, 2009
 */

class MakeTransparentEnhancerTest extends GroovyTestCase {

    public void testIsTransparent() {
        def items1 = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance items1
        assertFalse items1.isConcurrent()

        def items2 = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance items2
        assertTrue items2.makeTransparent().isConcurrent()
    }

    public void testIsTransparentWithString() {
        def items1 = '1abc'
        ParallelEnhancer.enhanceInstance items1
        assertFalse items1.isConcurrent()

        def items2 = '2abc'
        ParallelEnhancer.enhanceInstance items2
        assertTrue items2.makeTransparent().isConcurrent()
    }

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

        ParallelEnhancer.enhanceInstance(items)
        shouldFail {
            [1].makeTransparent()
        }
        assert items == items.makeTransparent()
        assertNotNull(items.makeTransparent())
        assert items.makeTransparent() == items.makeTransparent().makeTransparent()
        final def p1 = items.makeTransparent()
        assert p1 == p1.makeTransparent()

        shouldFail {
            [1].makeTransparent()
        }
    }

    public void testMakeTransparentTypeCompatibility() {
        Collection c1 = ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5])
        Collection c2 = ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5]).makeTransparent()
        String s1 = ParallelEnhancer.enhanceInstance('abcde')
        String s2 = ParallelEnhancer.enhanceInstance('abcde').makeTransparent()
        assert !c1.isEmpty()
        assert !c2.isEmpty()
        assert !s1.isEmpty()
        assert !s2.isEmpty()
    }

    public void testNonTransparentAfterClone() {
        GParsPool.withPool {
            Collection c = ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5]).makeTransparent()
            assert c.isConcurrent()
            assertFalse c.clone().isConcurrent()
        }
    }

    public void testMakeTransparentPropagationToResults() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance items
        items.makeTransparent().collect {it * 2}.findAll {it > 1}.each {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testIsTransparentCheck() {
        def items = [1, 2, 3, 4, 5]
        shouldFail {
            items.isConcurrent()
        }
        shouldFail() {
            items.makeTransparent()
        }
        ParallelEnhancer.enhanceInstance(items)
        assertFalse items.isConcurrent()
        shouldFail {
            'abc'.isConcurrent()
        }
        assertTrue items.makeTransparent().isConcurrent()
        assertTrue items.isConcurrent()
        def s = 'abcde'
        ParallelEnhancer.enhanceInstance(s)
        assertTrue s.makeTransparent().isConcurrent()

        assertTrue items.isConcurrent()
        assertTrue 'abcde'.isConcurrent()
        shouldFail {
            assertTrue 'ab'.isConcurrent()
        }
    }

    public void testIdempotenceOfNestingMakeTransparent() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().makeTransparent().each {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testNoMakeTransparentPropagationToResultsWithGroupBy() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().groupBy {it % 2}.each {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() == 1
    }

    public void testMakeTransparentPropagationToResultsWithString() {
        def items = 'abcde'
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().collect {it * 2}.findAll {it.size() > 1}.each {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testMakeTransparentPropagationToResultsWithIterator() {
        def items = [1, 2, 3, 4, 5].iterator()
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().collect {it * 2}.findAll {it > 1}.each {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testTransparentParallelInMethodCall() {
        def items = [1, 2, 3, 4, 5]
        assertEquals 1, foo(items).keys().size()

        ParallelEnhancer.enhanceInstance(items)
        assertEquals 1, foo(items).keys().size()
        assert foo(items.makeTransparent()).keys().size() > 1
    }

    private def foo(Collection c) {
        final Map map = new ConcurrentHashMap()
        c.collect {it * 2}.findAll {it > 1}.each {
            Thread.sleep 50
            map[Thread.currentThread()] = ''
        }
        return map
    }

    public void testTransparentMin() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().min {a, b ->
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return a - b
        }
        assert map.keys().size() > 1
    }

    public void testTransparentMax() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().max {a, b ->
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return a - b
        }
        assert map.keys().size() > 1
    }

    public void testTransparentSum() {
        def items = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance(items)
        assertEquals 15, items.makeTransparent().sum()
    }

    public void testTransparentCount() {
        assertEquals 1, ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5]).makeTransparent().count(3)
        assertEquals 5, ParallelEnhancer.enhanceInstance([3, 2, 3, 4, 5, 3, 3, 3]).makeTransparent().count(3)
        assertEquals 0, ParallelEnhancer.enhanceInstance([3, 2, 3, 4, 5, 3, 3, 3]).makeTransparent().count(6)
        assertEquals 0, ParallelEnhancer.enhanceInstance([]).makeTransparent().count(6)
        assertEquals 1, ParallelEnhancer.enhanceInstance('abc2').makeTransparent().count('a')
        assertEquals 3, ParallelEnhancer.enhanceInstance('abcaa2').makeTransparent().count('a')
        assertEquals 0, ParallelEnhancer.enhanceInstance('ebc2').makeTransparent().count('a')
        assertEquals 0, ParallelEnhancer.enhanceInstance('     '.trim()).makeTransparent().count('a')
    }

    public void testSplit() {
        def result = ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5]).makeTransparent().split {it > 2}
        assert [3, 4, 5] as Set == result[0] as Set
        assert [1, 2] as Set == result[1] as Set
        assertEquals 2, result.size()
        assert [[], []] == ParallelEnhancer.enhanceInstance([]).makeTransparent().split {it > 2}
        result = ParallelEnhancer.enhanceInstance([3]).makeTransparent().split {it > 2}
        assert [[3], []] == result
        result = ParallelEnhancer.enhanceInstance([1]).makeTransparent().split {it > 2}
        assert [[], [1]] == result
    }

    public void testSplitOnString() {
        def result = ParallelEnhancer.enhanceInstance(new String('abc')).makeTransparent().split {it == 'b'}
        assert ['b'] as Set == result[0] as Set
        assert ['a', 'c'] as Set == result[1] as Set
        assertEquals 2, result.size()
        result = ParallelEnhancer.enhanceInstance('').makeTransparent().split {it == 'b'}
        assert [[], []] == result
        result = ParallelEnhancer.enhanceInstance('b').makeTransparent().split {it == 'b'}
        assert [['b'], []] == result
        result = ParallelEnhancer.enhanceInstance('a').makeTransparent().split {it == 'b'}
        assert [[], ['a']] == result
    }

    public void testTransparentReduce() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().fold {a, b ->
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return a + b
        }
        assert map.keys().size() > 1
    }

    public void testTransparentSeededReduce() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().fold(10) {a, b ->
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return a + b
        }
        assert map.keys().size() > 1
    }
}
