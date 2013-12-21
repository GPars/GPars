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

/**
 * Author: Vaclav Pech
 * Date: Oct 30, 2009
 */

@SuppressWarnings("SpellCheckingInspection")
class MakeTransparentEnhancerTest extends GroovyTestCase {

    public void testIsTransparent() {
        def items1 = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance items1
        assertFalse items1.isConcurrent()

        def items2 = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance items2
        assertTrue items2.makeConcurrent().isConcurrent()
    }

    public void testIsTransparentWithString() {
        def items1 = '1abc'
        ParallelEnhancer.enhanceInstance items1
        assertFalse items1.isConcurrent()

        def items2 = '2abc'
        ParallelEnhancer.enhanceInstance items2
        assertTrue items2.makeConcurrent().isConcurrent()
    }

    public void testMakeTransparentAvailability() {
        shouldFail {
            [].makeConcurrent()
            [1].makeConcurrent()
            'abcde'.makeConcurrent()
        }

        def items = [1, 2, 3, 4, 5]

        shouldFail {
            items.makeConcurrent()
        }

        ParallelEnhancer.enhanceInstance(items)
        shouldFail {
            [1].makeConcurrent()
        }
        assert items == items.makeConcurrent()
        assertNotNull(items.makeConcurrent())
        assert items.makeConcurrent() == items.makeConcurrent().makeConcurrent()
        final def p1 = items.makeConcurrent()
        assert p1 == p1.makeConcurrent()

        shouldFail {
            [1].makeConcurrent()
        }
    }

    public void testMakeTransparentTypeCompatibility() {
        Collection c1 = ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5])
        Collection c2 = ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5]).makeConcurrent()
        String s1 = ParallelEnhancer.enhanceInstance('abcde')
        String s2 = ParallelEnhancer.enhanceInstance('abcde').makeConcurrent()
        assert !c1.isEmpty()
        assert !c2.isEmpty()
        assert !s1.isEmpty()
        assert !s2.isEmpty()
    }

    public void testNonTransparentAfterClone() {
        GParsPool.withPool {
            Collection c = ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5]).makeConcurrent()
            assert c.isConcurrent()
            assertFalse c.clone().isConcurrent()
        }
    }

    public void testMakeTransparentPropagationToResults() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance items
        items.makeConcurrent().collect { it * 2 }.findAll { it > 1 }.each {
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
            items.makeConcurrent()
        }
        ParallelEnhancer.enhanceInstance(items)
        assertFalse items.isConcurrent()
        shouldFail {
            'abc'.isConcurrent()
        }
        assertTrue items.makeConcurrent().isConcurrent()
        assertTrue items.isConcurrent()
        def s = 'abcde'
        ParallelEnhancer.enhanceInstance(s)
        assertTrue s.makeConcurrent().isConcurrent()

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
        items.makeConcurrent().makeConcurrent().each {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testNoMakeTransparentPropagationToResultsWithGroupBy() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().groupBy { it % 2 }.each {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() == 1
    }

    public void testMakeTransparentPropagationToResultsWithString() {
        def items = 'abcde'
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().collect { it * 2 }.findAll { it.size() > 1 }.each {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testMakeTransparentPropagationToResultsWithIterator() {
        def items = [1, 2, 3, 4, 5].iterator()
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().collect { it * 2 }.findAll { it > 1 }.each {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testTransparentParallelInMethodCall() {
        def items = [1, 2, 3, 4, 5]
        assert 1 == foo(items).keys().size()

        ParallelEnhancer.enhanceInstance(items)
        assert 1 == foo(items).keys().size()
        assert foo(items.makeConcurrent()).keys().size() > 1
    }

    private def foo(Collection c) {
        final Map map = new ConcurrentHashMap()
        c.collect { it * 2 }.findAll { it > 1 }.each {
            Thread.sleep 50
            map[Thread.currentThread()] = ''
        }
        return map
    }

    public void testTransparentMin() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().min { a, b ->
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
        items.makeConcurrent().max { a, b ->
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return a - b
        }
        assert map.keys().size() > 1
    }

    public void testTransparentSum() {
        def items = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance(items)
        assert 15 == items.makeConcurrent().sum()
    }

    public void testTransparentCount() {
        assert 1 == groovyx.gpars.ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5]).makeConcurrent().count(3)
        assert 5 == groovyx.gpars.ParallelEnhancer.enhanceInstance([3, 2, 3, 4, 5, 3, 3, 3]).makeConcurrent().count(3)
        assert 0 == groovyx.gpars.ParallelEnhancer.enhanceInstance([3, 2, 3, 4, 5, 3, 3, 3]).makeConcurrent().count(6)
        assert 0 == groovyx.gpars.ParallelEnhancer.enhanceInstance([]).makeConcurrent().count(6)
        assert 1 == groovyx.gpars.ParallelEnhancer.enhanceInstance('abc2').makeConcurrent().count('a')
        assert 3 == groovyx.gpars.ParallelEnhancer.enhanceInstance('abcaa2').makeConcurrent().count('a')
        assert 0 == groovyx.gpars.ParallelEnhancer.enhanceInstance('ebc2').makeConcurrent().count('a')
        assert 0 == groovyx.gpars.ParallelEnhancer.enhanceInstance('     '.trim()).makeConcurrent().count('a')
    }

    public void testSplit() {
        def result = ParallelEnhancer.enhanceInstance([1, 2, 3, 4, 5]).makeConcurrent().split { it > 2 }
        assert [3, 4, 5] as Set == result[0] as Set
        assert [1, 2] as Set == result[1] as Set
        assert 2 == result.size()
        assert [[], []] == ParallelEnhancer.enhanceInstance([]).makeConcurrent().split { it > 2 }
        result = ParallelEnhancer.enhanceInstance([3]).makeConcurrent().split { it > 2 }
        assert [[3], []] == result
        result = ParallelEnhancer.enhanceInstance([1]).makeConcurrent().split { it > 2 }
        assert [[], [1]] == result
    }

    public void testSplitOnString() {
        def result = ParallelEnhancer.enhanceInstance(new String('abc')).makeConcurrent().split { it == 'b' }
        assert ['b'] as Set == result[0] as Set
        assert ['a', 'c'] as Set == result[1] as Set
        assert 2 == result.size()
        result = ParallelEnhancer.enhanceInstance('').makeConcurrent().split { it == 'b' }
        assert [[], []] == result
        result = ParallelEnhancer.enhanceInstance('b').makeConcurrent().split { it == 'b' }
        assert [['b'], []] == result
        result = ParallelEnhancer.enhanceInstance('a').makeConcurrent().split { it == 'b' }
        assert [[], ['a']] == result
    }

    public void testTransparentReduce() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().inject { a, b ->
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
        items.makeConcurrent().inject(10) { a, b ->
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return a + b
        }
        assert map.keys().size() > 1
    }
}
