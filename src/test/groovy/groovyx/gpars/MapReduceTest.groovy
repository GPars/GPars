// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012, 2017  The original author or authors
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
 * @author Vaclav Pech
 * Date: Nov 6, 2009
 */

@SuppressWarnings("SpellCheckingInspection")
public class MapReduceTest extends groovy.test.GroovyTestCase {

    public void testReduce() {
        GParsPool.withPool(5) {
            assert 15 == [1, 2, 3, 4, 5].parallel.map { it }.reduce { a, b -> a + b }
            assert 'abc' == 'abc'.parallel.map { it }.reduce { a, b -> a + b }
            assert 55 == [1, 2, 3, 4, 5].parallel.map { it**2 }.reduce { a, b -> a + b }
            assert 'aa:bb:cc:dd:ee' == 'abcde'.parallel.map { it * 2 }.reduce { a, b -> "$a:$b" }
            assert 'aa-bb-dd' == 'abcde'.parallel.filter { it != 'e' }.map { it * 2 }.filter {
                it != 'cc'
            }.reduce { a, b -> "$a-$b" }
        }
    }

    @SuppressWarnings("GroovyMethodWithMoreThanThreeNegations")
    public void testFilterOperations() {
        GParsPool.withPool(5) {
            assert 'aa' == 'abcde'.parallel.filter { it != 'e' }.map { it * 2 }.filter { it != 'cc' }.min()
            assert 'dd' == 'abcde'.parallel.filter { it != 'e' }.map { it * 2 }.filter { it != 'cc' }.max()
            assert 'aabbdd' == 'abcde'.parallel.filter { it != 'e' }.map { it * 2 }.filter { it != 'cc' }.sum()
            assert 3 == 'abcde'.parallel.filter { it != 'e' }.map { it * 2 }.filter { it != 'cc' }.size()
            assert 4 == 'abcde'.parallel.filter { it != 'e' }.map { it.size() * 2 }.size()
            assert 4 == 'abcde'.parallel.filter { it != 'e' }.map { it.size() * 2 }.collection.size()
        }
    }

    public void testSeededReduce() {
        GParsPool.withPool(5) {
            assert 15 == [1, 2, 3, 4, 5].parallel.map { it }.reduce(0) { a, b -> a + b }
            assert 25 == [1, 2, 3, 4, 5].parallel.map { it }.reduce(10) { a, b -> a + b }
            assert 'abc' == 'abc'.parallel.map { it }.reduce('') { a, b -> a + b }
            assert 'abcd' == 'abc'.parallel.map { it }.reduce('d') { a, b -> a + b }
        }
    }

    public void testNestedMap() {
        GParsPool.withPool(5) {
            assert 65 == [1, 2, 3, 4, 5].parallel.map { it }.map { it + 10 }.reduce { a, b -> a + b }
        }
    }

    public void testMapFilter() {
        GParsPool.withPool(5) {
            assert ([4, 5].containsAll([1, 2, 3, 4, 5].parallel.map { it }.filter { it > 3 }.collection))
            assert 9 == [1, 2, 3, 4, 5].parallel.map { it }.filter { it > 3 }.map { it }.reduce { a, b -> a + b }
        }
    }

    public void testFilterMap() {
        GParsPool.withPool(5) {
            assert 9 == [1, 2, 3, 4, 5].parallel.filter { it > 3 }.map { it }.reduce { a, b -> a + b }
        }
    }

    public void testReduceThreads() {
        final Map map = new ConcurrentHashMap()

        GParsPool.withPool(5) {
            assert 55 == [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].parallel.map { it }.reduce { a, b ->
                Thread.sleep 200
                map[Thread.currentThread()] = ''
                a + b
            }
            assert map.keys().size() > 1
        }
    }

    public void testMinMax() {
        GParsPool.withPool(5) {
            assert 1 == [1, 2, 3, 4, 5].parallel.map { it }.min { a, b -> a - b }
            assert 1 == [1, 2, 3, 4, 5].parallel.map { it }.min { it }
            assert 1 == [1, 2, 3, 4, 5].parallel.map { it }.min()
            assert 5 == [1, 2, 3, 4, 5].parallel.map { it }.max { a, b -> a - b }
            assert 5 == [1, 2, 3, 4, 5].parallel.map { it }.max { it }
            assert 5 == [1, 2, 3, 4, 5].parallel.map { it }.max()
            assert 'a' == 'abc'.parallel.map { it }.min()
            assert 'c' == 'abc'.parallel.map { it }.max()
        }
    }

    public void testSum() {
        GParsPool.withPool(5) {
            assert 15 == [1, 2, 3, 4, 5].parallel.sum()
            assert 'aabbccddee' == 'abcde'.parallel.map { it * 2 }.sum()
        }
    }

    public void testCollectionProperty() {
        GParsPool.withPool(5) {
            final def original = [1, 2, 3, 4, 5]
            final def collection = original.parallel.collection
            assert original == collection
            assert original.is(collection)
            assert collection instanceof ArrayList
            assert collection == collection.clone()
        }
    }

    public void testSort() {
        GParsPool.withPool(5) {
            final List sortedNums = [1, 2, 3, 4, 5]
            assert sortedNums == [1, 2, 3, 4, 5].parallel.map { it }.sort { a, b -> a - b }.collection
            assert sortedNums == [3, 5, 1, 2, 4].parallel.map { it }.sort { a, b -> a - b }.collection
            assert sortedNums == [3, 5, 1, 4, 2].parallel.map { it }.sort { it }.collection
            assert sortedNums == [3, 5, 1, 2, 4].parallel.map { it }.sort().collection
            assert 'abc' == 'cba'.parallel.map { it }.sort().collection.join('')
            assert 'abc' == 'bac'.parallel.map { it }.sort().collection.join('')
        }
    }

    public void testGroupBy() {
        GParsPool.withPool(5) {
            assert [1, 2, 3, 4, 5].parallel.groupBy { it > 2 }.size() == 2
            assert [4, 2, 3, 1, 5].parallel.groupBy { Number number -> 1 }.size() == 1
            assert [2, 4, 5, 1, 3].parallel.groupBy { Number number -> number }.size() == 5
            final def groups = [1, 2, 3, 4, 5].parallel.groupBy { Number number -> number % 2 }
            assert groups.size() == 2
            assert (groups[0].containsAll([2, 4]) && groups[0].size() == 2) || (groups[0].containsAll([1, 3, 5]) && groups[0].size() == 3)
            assert (groups[1].containsAll([2, 4]) && groups[1].size() == 2) || (groups[1].containsAll([1, 3, 5]) && groups[1].size() == 3)

        }
    }
}
