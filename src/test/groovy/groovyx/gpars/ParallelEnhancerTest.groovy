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

import java.util.concurrent.ConcurrentHashMap

public class ParallelEnhancerTest extends GroovyTestCase {
    public void testInstanceEnhancement() {
        final List list = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance list
        assert list.everyParallel {it > 0}
        assertEquals 1, list.findParallel {it < 2}
        assertEquals([1, 2], list.findAllParallel {it < 3})
        assertEquals([2, 4, 6, 8, 10], list.collectParallel {2 * it})
        def result = Collections.synchronizedSet(new HashSet())
        list.eachParallel {result << 2 * it}
        assertEquals(new HashSet([2, 4, 6, 8, 10]), result)
    }

    public void testClassEnhancement() {
        ParallelEnhancer.enhanceClass LinkedList
        final List list = new LinkedList([1, 2, 3, 4, 5])
        assert list.anyParallel {it > 4}
        assert list.everyParallel {it > 0}
        assertEquals 1, list.findParallel {it < 2}
        assertEquals([1, 2], list.findAllParallel {it < 3})
        assertEquals([2, 4, 6, 8, 10], list.collectParallel {2 * it})
        def result = Collections.synchronizedSet(new HashSet())
        list.eachParallel {result << 2 * it}
        assertEquals(5, result.size())
        assertEquals(new HashSet([2, 4, 6, 8, 10]), result)
    }

    public void testMapInstanceEnhancement() {
        final Map map = [1: 1, 2: 2, 3: 3, 4: 4, 5: 5]
        ParallelEnhancer.enhanceInstance map
        assert map.anyParallel {it.key > 4}
        assert map.everyParallel {it.value > 0}
    }

    public void testMapClassEnhancement() {
        ParallelEnhancer.enhanceClass HashMap
        final Map map = new HashMap([1: 1, 2: 2, 3: 3, 4: 4, 5: 5])
        assert map.anyParallel {it.key > 4}
        assert map.everyParallel {it.value > 0}
    }

    public void testInstanceEnhancementException() {
        final List list = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance list
        performExceptionCheck(list)
    }

    public void testClassEnhancementException() {
        ParallelEnhancer.enhanceClass LinkedList
        performExceptionCheck(new LinkedList([1, 2, 3, 4, 5]))
    }

    public void testDualEnhancement() {
        ParallelEnhancer.enhanceClass LinkedList
        final List list = new LinkedList([1, 2, 3, 4, 5])
        assertEquals([2, 4, 6, 8, 10], list.collectParallel {2 * it})

        ParallelEnhancer.enhanceInstance list
        assertEquals([2, 4, 6, 8, 10], list.collectParallel {2 * it})

        assertEquals([2, 4, 6, 8, 10], new LinkedList([1, 2, 3, 4, 5]).collectParallel {2 * it})
    }

    private String performExceptionCheck(List list) {
        shouldFail(IllegalArgumentException) {list.anyParallel {if (it > 4) throw new IllegalArgumentException('test') else false}}
        shouldFail(IllegalArgumentException) {list.everyParallel {if (it > 4) throw new IllegalArgumentException('test') else true}}
        shouldFail(IllegalArgumentException) {list.findParallel {if (it > 4) throw new IllegalArgumentException('test') else false}}
        shouldFail(IllegalArgumentException) {list.findAllParallel {if (it > 4) throw new IllegalArgumentException('test') else true}}
        shouldFail(IllegalArgumentException) {list.collectParallel {if (it > 4) throw new IllegalArgumentException('test') else 1}}
        shouldFail(IllegalArgumentException) {list.eachParallel {if (it > 4) throw new IllegalArgumentException('test')}}
    }

    public void testEnhancementPropagationToResults() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance items
        items.collectParallel {it * 2}.findAllParallel {it > 1}.eachParallel {
            Thread.sleep 500
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 2
    }

    public void testMin() {
        final List list = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance list
        assertEquals 1, list.minParallel {a, b -> a - b}
        assertEquals 1, list.minParallel {it}
        assertEquals 5, list.minParallel {a, b -> b - a}
        assertEquals 1, list.minParallel()
    }

    public void testMax() {
        final List list = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance list

        assertEquals 5, list.maxParallel {a, b -> a - b}
        assertEquals 5, list.maxParallel {it}
        assertEquals 1, list.maxParallel {a, b -> b - a}
        assertEquals 5, list.maxParallel()
    }

    public void testSum() {
        final List list = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance list

        assertEquals 15, list.sumParallel()

        def s = 'aaaaabbbbccccc'
        ParallelEnhancer.enhanceInstance s
        assertEquals 'aaaaabbbbccccc', s.sumParallel()
    }

    public void testReduce() {
        final List list = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance list

        assertEquals 15, list.foldParallel() {a, b -> a + b}
        assertEquals 55, list.collectParallel {it ** 2}.foldParallel {a, b -> a + b}
    }

    public void testReduceThreads() {
        final ConcurrentHashMap map = new ConcurrentHashMap()

        final List list = [1, 2, 3, 4, 5]
        ParallelEnhancer.enhanceInstance list

        assertEquals 15, list.foldParallel {a, b ->
            Thread.sleep 200
            map[Thread.currentThread()] = ''
            a + b
        }
        assert map.keys().size() > 1
    }
}
