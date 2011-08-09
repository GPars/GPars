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

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class GParsExecutorsPoolOnMapTest extends GroovyTestCase {

    public void testMapSpecificsForEach() {
        def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
        GParsExecutorsPool.withPool {
            final def keyResults = [].asSynchronized()
            final def valueResults = [].asSynchronized()
            map.eachParallel {item -> keyResults << item.key; valueResults << item.value}
            processResults(keyResults, valueResults)

            map.eachParallel {k, v -> keyResults << k; valueResults << v}
            processResults(keyResults, valueResults)
        }
    }

    public void testMapSpecificsForEachWithIndex() {
        def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
        GParsExecutorsPool.withPool {
            final def keyResults = [].asSynchronized()
            final def valueResults = [].asSynchronized()
            map.eachWithIndexParallel {item, index -> keyResults << item.key; valueResults << item.value}
            processResults(keyResults, valueResults)

            map.eachWithIndexParallel {k, v, index -> keyResults << k; valueResults << v}
            processResults(keyResults, valueResults)
        }
    }

    public void testMapSpecificsForCollect() {
        def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
        GParsExecutorsPool.withPool {
            final def keyResults = [].asSynchronized()
            final def valueResults = [].asSynchronized()

            keyResults = map.collectParallel {item -> item.key}
            valueResults = map.collectParallel {item -> item.value}
            processResults(keyResults, valueResults)

            keyResults = map.collectParallel {k, v -> k}
            valueResults = map.collectParallel {k, v -> v}
            processResults(keyResults, valueResults)
        }
    }

    public void testMapSpecificsForAny() {
        def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
        GParsExecutorsPool.withPool {
            assert map.anyParallel {item -> item.key == 'c'}
            assert map.anyParallel {k, v -> k == 'c'}
            assert map.anyParallel {item -> item.value > 3}
            assert map.anyParallel {k, v -> v > 3}
        }
    }

    public void testMapSpecificsForEvery() {
        def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
        GParsExecutorsPool.withPool {
            assert !map.everyParallel {item -> item.key == 'c'}
            assert !map.everyParallel {k, v -> k == 'c'}
            assert map.everyParallel {item -> item.value > 0}
            assert map.everyParallel {k, v -> v > 0}
        }
    }

    public void testMapSpecificsForFindAny() {
        def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
        GParsExecutorsPool.withPool {
            assert map.findAnyParallel {item -> item.key == 'c'}.key == 'c'
            assert map.findAnyParallel {k, v -> k == 'c'}.value == 3
            assert map.findAnyParallel {item -> item.value > 3}.key in ['d', 'e']
            assert map.findAnyParallel {k, v -> v.value > 3}.value in [4, 5]
        }
    }

    public void testMapSpecificsForFind() {
        def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
        GParsExecutorsPool.withPool {
            assert map.findParallel {item -> item.key == 'c'}.key == 'c'
            assert map.findParallel {k, v -> k == 'c'}.value == 3
            assert map.findParallel {item -> item.value > 3}.key == 'd'
            assert map.findParallel {k, v -> v.value > 3}.value == 4
        }
    }

    public void testMapSpecificsForFindAll() {
        def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
        GParsExecutorsPool.withPool {
            assert map.findAllParallel {item -> item.key == 'c'} == ['c': 3]
            assert map.findAllParallel {k, v -> k == 'c'} == ['c': 3]
            assert map.findAllParallel {item -> item.value > 3} == ['d': 4, 'e': 5]
            assert map.findAllParallel {k, v -> v.value > 3} == ['d': 4, 'e': 5]
        }
    }

    public void testMapSpecificsForGrep() {
        def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
        GParsExecutorsPool.withPool {
            assert map.grepParallel {item -> item.key == 'c'} == ['c': 3]
            assert map.grepParallel {k, v -> k == 'c'} == ['c': 3]
            assert map.grepParallel {item -> item.value > 3} == ['d': 4, 'e': 5]
            assert map.grepParallel {k, v -> v.value > 3} == ['d': 4, 'e': 5]
            assert map.grepParallel(['d': 4].entrySet().iterator().next()) == ['d': 4]
        }
    }

    private def processResults(List keyResults, List valueResults) {
        assert keyResults.containsAll(['a', 'b', 'c', 'd', 'e'])
        assert keyResults.size() == 5
        keyResults.clear()

        assert valueResults.containsAll([1, 2, 3, 4, 5])
        assert valueResults.size() == 5
        valueResults.clear()
    }
}
