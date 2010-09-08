// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

public class MapReduceTest extends GroovyTestCase {

    public void testReduce() {
        GParsPool.withPool(5) {
            assertEquals 15, [1, 2, 3, 4, 5].parallel.map {it}.reduce {a, b -> a + b}
            assertEquals 'abc', 'abc'.parallel.map {it}.reduce {a, b -> a + b}
            assertEquals 55, [1, 2, 3, 4, 5].parallel.map {it ** 2}.reduce {a, b -> a + b}
            assertEquals 'aa:bb:cc:dd:ee', 'abcde'.parallel.map {it * 2}.reduce {a, b -> "$a:$b"}
            assertEquals 'aa-bb-dd', 'abcde'.parallel.filter {it != 'e'}.map {it * 2}.filter {it != 'cc'}.reduce {a, b -> "$a-$b"}
        }
    }

    @SuppressWarnings("GroovyMethodWithMoreThanThreeNegations")
    public void testFilterOperations() {
        GParsPool.withPool(5) {
            assertEquals 'aa', 'abcde'.parallel.filter {it != 'e'}.map {it * 2}.filter {it != 'cc'}.min()
            assertEquals 'dd', 'abcde'.parallel.filter {it != 'e'}.map {it * 2}.filter {it != 'cc'}.max()
            assertEquals 'aabbdd', 'abcde'.parallel.filter {it != 'e'}.map {it * 2}.filter {it != 'cc'}.sum()
            assertEquals 3, 'abcde'.parallel.filter {it != 'e'}.map {it * 2}.filter {it != 'cc'}.size()
            assertEquals 4, 'abcde'.parallel.filter {it != 'e'}.map {it.size() * 2}.size()
            assertEquals 4, 'abcde'.parallel.filter {it != 'e'}.map {it.size() * 2}.collection.size()
        }
    }

    public void testSeededReduce() {
        GParsPool.withPool(5) {
            assertEquals 15, [1, 2, 3, 4, 5].parallel.map {it}.reduce(0) {a, b -> a + b}
            assertEquals 25, [1, 2, 3, 4, 5].parallel.map {it}.reduce(10) {a, b -> a + b}
            assertEquals 'abc', 'abc'.parallel.map {it}.reduce('') {a, b -> a + b}
            assertEquals 'abcd', 'abc'.parallel.map {it}.reduce('d') {a, b -> a + b}
        }
    }

    public void testNestedMap() {
        GParsPool.withPool(5) {
            assertEquals 65, [1, 2, 3, 4, 5].parallel.map {it}.map {it + 10}.reduce {a, b -> a + b}
        }
    }

    public void testMapFilter() {
        GParsPool.withPool(5) {
            assert ([4, 5].containsAll([1, 2, 3, 4, 5].parallel.map {it}.filter {it > 3}.collection))
            assertEquals 9, [1, 2, 3, 4, 5].parallel.map {it}.filter { it > 3 }.map {it}.reduce {a, b -> a + b }
        }
    }

    public void testFilterMap() {
        GParsPool.withPool(5) {
            assertEquals 9, [1, 2, 3, 4, 5].parallel.filter {it > 3}.map {it}.reduce {a, b -> a + b}
        }
    }

    public void testReduceThreads() {
        final ConcurrentHashMap map = new ConcurrentHashMap()

        GParsPool.withPool(5) {
            assertEquals 55, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].parallel.map {it}.reduce {a, b ->
                Thread.sleep 200
                map[Thread.currentThread()] = ''
                a + b
            }
            assert map.keys().size() > 1
        }
    }

    public void testMinMax() {
        GParsPool.withPool(5) {
            assertEquals 1, [1, 2, 3, 4, 5].parallel.map {it}.min {a, b -> a - b}
            assertEquals 1, [1, 2, 3, 4, 5].parallel.map {it}.min {it}
            assertEquals 1, [1, 2, 3, 4, 5].parallel.map {it}.min()
            assertEquals 5, [1, 2, 3, 4, 5].parallel.map {it}.max {a, b -> a - b}
            assertEquals 5, [1, 2, 3, 4, 5].parallel.map {it}.max {it}
            assertEquals 5, [1, 2, 3, 4, 5].parallel.map {it}.max()
            assertEquals 'a', 'abc'.parallel.map {it}.min()
            assertEquals 'c', 'abc'.parallel.map {it}.max()
        }
    }

    public void testSum() {
        GParsPool.withPool(5) {
            assertEquals 15, [1, 2, 3, 4, 5].parallel.sum()
            assertEquals 'aabbccddee', 'abcde'.parallel.map {it * 2}.sum()
        }
    }

    public void testCollectionProperty() {
        GParsPool.withPool(5) {
            final def original = [1, 2, 3, 4, 5]
            final def collection = original.parallel.collection
            assertEquals original, collection
            assert !original.is(collection)
            assert collection instanceof ArrayList
            assertEquals collection, collection.clone()
        }
    }

    public void testSort() {
        GParsPool.withPool(5) {
            final List sortedNums = [1, 2, 3, 4, 5]
            assertEquals(sortedNums, [1, 2, 3, 4, 5].parallel.map {it}.sort {a, b -> a - b}.collection)
            assertEquals(sortedNums, [3, 5, 1, 2, 4].parallel.map {it}.sort {a, b -> a - b}.collection)
            assertEquals sortedNums, [3, 5, 1, 4, 2].parallel.map {it}.sort {it}.collection
            assertEquals sortedNums, [3, 5, 1, 2, 4].parallel.map {it}.sort().collection
            assertEquals 'abc', 'cba'.parallel.map {it}.sort().collection.join('')
            assertEquals 'abc', 'bac'.parallel.map {it}.sort().collection.join('')
        }
    }

    public void testGroupBy() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert [1, 2, 3, 4, 5].parallel.groupBy {it > 2}.size() == 2
            assert [4, 2, 3, 1, 5].parallel.groupBy {Number number -> 1}.size() == 1
            assert [2, 4, 5, 1, 3].parallel.groupBy {Number number -> number}.size() == 5
            final def groups = [1, 2, 3, 4, 5].parallel.groupBy {Number number -> number % 2}
            assert groups.size() == 2
            assert (groups[0].containsAll([2, 4]) && groups[0].size() == 2) || (groups[0].containsAll([1, 3, 5]) && groups[0].size() == 3)
            assert (groups[1].containsAll([2, 4]) && groups[1].size() == 2) || (groups[1].containsAll([1, 3, 5]) && groups[1].size() == 3)

        }
    }

    public void testCombine() {
        def words = """The xxxParallel() methods have to follow the contract of their non-parallel peers. So a collectParallel() method must return a legal collection of items, which you can again treat as a Groovy collection. Internally the parallel collect method builds an efficient parallel structure, called parallel array, performs the required operation concurrently and before returning destroys the Parallel Array building the collection of results to return to you. A potential call to let say findAllParallel() on the resulting collection would repeat the whole process of construction and destruction of a Parallel Array instance under the covers. With Map/Reduce you turn your collection into a Parallel Array and back only once. The Map/Reduce family of methods do not return Groovy collections, but are free to pass along the internal Parallel Arrays directly. Invoking the parallel property on a collection will build a Parallel Array for the collection and return a thin wrapper around the Parallel Array instance. Then you can chain all required methods like:""".tokenize()
        groovyx.gpars.GParsPool.withPool(5) {
            def result1 = words.parallel.map {[it, 1]}.combine(0, {a, b -> a + b}).getParallel().sort {-it.value}.collection
            def result2 = words.parallel.map {[it, 1]}.combine({0}, {a, b -> a + b}).getParallel().sort {-it.value}.collection
            def result3 = words.parallel.map {[it, 1]}.combine([], {list, value -> list << value}).getParallel().map {it.value = it.value.size(); it}.sort {-it.value}.collection

            assert [result1, result2, result3]*.size() == [101, 101, 101]
            assert result1 == result2
            assert result1 == result3
            assert result1[0].key == 'the'
            assert result1[0].value == 12
        }
    }
}