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
public class MapReduceTest extends GroovyTestCase {

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
            assert !original.is(collection)
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

    public void testCombine() {
        def words = """The xxxParallel() methods have to follow the contract of their non-parallel peers. So a collectParallel() method must return a legal collection of items, which you can again treat as a Groovy collection. Internally the parallel collect method builds an efficient parallel structure, called parallel array, performs the required operation concurrently and before returning destroys the Parallel Array building the collection of results to return to you. A potential call to let say findAllParallel() on the resulting collection would repeat the whole process of construction and destruction of a Parallel Array instance under the covers. With Map/Reduce you turn your collection into a Parallel Array and back only once. The Map/Reduce family of methods do not return Groovy collections, but are free to pass along the internal Parallel Arrays directly. Invoking the parallel property on a collection will build a Parallel Array for the collection and return a thin wrapper around the Parallel Array instance. Then you can chain all required methods like:""".tokenize()
        GParsPool.withPool(5) {
            def result1 = words.parallel.map { [it, 1] }.combine(0, { a, b -> a + b }).getParallel().sort {
                -it.value
            }.collection
            def result2 = words.parallel.map { [it, 1] }.combine({ 0 }, { a, b -> a + b }).getParallel().sort {
                -it.value
            }.collection
            def result3 = words.parallel.map {
                [it, 1]
            }.combine([], { list, value -> list << value }).getParallel().map { it.value = it.value.size(); it }.sort {
                -it.value
            }.collection

            assert [result1, result2, result3]*.size() == [101, 101, 101]
            assert result1 == result2
            assert result1 == result3
            assert result1[0].key == 'the'
            assert result1[0].value == 12
        }
    }

    public void testSingleElementCombine1() {
        def list = [
                [key1: 'ABC', key2: 3, value: 1.01],
                [key1: 'ABC', key2: 4, value: 1.02],
                [key1: 'ABC', key2: 4, value: 1.03],
                [key1: 'DEF', key2: 3, value: 1.04]]

        GParsPool.withPool {
            def mapInner = { entrylist ->
                GParsPool.withPool {
                    entrylist.parallel.map { [it.key2, it.value] }.combine(0) { acc, v -> acc + v }
                }
            }

//for dealing with bug when only 1 list item
            def collectSingle = { entrylist -> def first = entrylist[0]; return [(first.key2): first.value] }
            def result1 = list.parallel.groupBy { it.key1 }.getParallel()
                    .map { [(it.key): (it.value?.size()) > 1 ? mapInner.call(it.value) : collectSingle.call(it.value)] }
                    .reduce([:]) { a, b -> a + b }
            def result2 = list.parallel.groupBy { it.key1 }.getParallel()
                    .map { [(it.key): mapInner.call(it.value)] }
                    .reduce([:]) { a, b -> a + b }
            assert result1 == result2
        }
    }

    public void testSingleElementCombine2() {
        def working = [
                [id: 1, key1: "fred", total: 10, date: "2012-01-01", key2: 1],
                [id: 2, key1: "fred", total: 10.12, date: "2012-01-03", key2: 1],
                [id: 4, key1: "fred", total: 10, date: "2012-02-11", key2: 2],
                [id: 3, key1: "jane", total: 10, date: "2012-01-04", key2: 1],
                [id: 5, key1: "jane", total: 10, date: "2012-01-01", key2: 1],
                [id: 6, key1: "ted", total: 10, date: "2012-03-21", key2: 3],
                [id: 7, key1: "ted", total: 10, date: "2012-02-09", key2: 2]
        ]

        def notworking = [
                [id: 1, key1: "fred", total: 10, date: "2012-01-01", key2: 1],
                [id: 2, key1: "fred", total: 10.12, date: "2012-01-03", key2: 1],
                [id: 4, key1: "fred", total: 10, date: "2012-02-11", key2: 2],
                [id: 5, key1: "jane", total: 10, date: "2012-01-01", key2: 1],        //only 1 entry for Jane here
                [id: 6, key1: "ted", total: 10, date: "2012-03-21", key2: 3],
                [id: 7, key1: "ted", total: 10, date: "2012-02-09", key2: 2]
        ]

        def wrapUpList = { lst ->
            GParsPool.withPool {
                def mapInner = { entrylist ->
                    GParsPool.withPool {
                        entrylist.getParallel()
                                .map { [it.key2, it.total] }
                                .combine(0) { acc, v -> acc + v }
                    }
                }

                def result = lst.parallel
                        .groupBy { it.key1 }.getParallel()
                        .map { [(it.key): mapInner.call(it.value)] }
                        .reduce([:]) { a, b -> a + b }

                return result
            }
        }

//working block
        def result1 = wrapUpList(working)
        println "result1 = $result1"
        assert result1['fred'].size() == 2
        assert result1['fred'][1] == 20.12

        def result2 = wrapUpList(notworking)
        println "result2 = $result2"
        assert result2['fred'].size() == 2
        assert result2['fred'][1] == 20.12
    }
}
