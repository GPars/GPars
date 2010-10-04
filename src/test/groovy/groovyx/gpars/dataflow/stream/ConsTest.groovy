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

package groovyx.gpars.dataflow.stream

class ConsTest extends GroovyTestCase {

    void testEmptyList() {
        def list = Cons.EMPTY
        assert list.isEmpty()
    }

    void testListWithOneElement() {
        def list = new Cons<String>("first", Cons.EMPTY);
        assert !list.isEmpty()
        assert list.first == "first"
        assert list.rest.isEmpty()
    }

    void testListWithSeveralElements() {
        FList<String> list = new Cons<String>("first", new Cons<String>("second", new Cons<String>("third", Cons.EMPTY)));
        assert list.first == "first"
        assert list.rest.first == "second"
        assert list.rest.rest.first == "third"
    }

    void testCreateFromCollection() {
        FList<Integer> list = Cons.from([1, 2, 3, 4] as List<Integer>)
        assert list.first == 1
        assert list.rest.rest.rest.first == 4
        assert list.rest.rest.rest.rest.isEmpty()
    }

    void testConsFromEmptyCollection() {
        def list = Cons.from([])
        assert list.isEmpty()
    }

    void testConsIsIterable() {
        def list = Cons.from([1, 2, 3])
        def result = []
        for (number in list) {
            result << number
        }
        assert result == [1, 2, 3]
    }

    void testMapWithIdentity() {
        def list = Cons.from([1, 2, 3])
        def mappedList = list.map({it})
        assert mappedList == list
    }

    void testMapWithTransformation() {
        def list = Cons.from([1, 2, 3])
        def mappedList = list.map({it * 2})
        assert mappedList == Cons.from([2, 4, 6])
    }

    void testFilterNothing() {
        def list = Cons.from([1, 2, 3])
        def filteredList = list.filter({true})
        assert filteredList == list
    }

    void testFilterEverything() {
        def list = Cons.from([1, 2, 3])
        def filteredList = list.filter({false})
        assert filteredList.isEmpty()
    }

    void testFilterSomething() {
        def list = Cons.from([1, 2, 3, 4])
        def filteredList = list.filter({(it % 2) == 0})
        assert filteredList == Cons.from([2, 4])
    }

    void testReduceEmptyList() {
        def list = Cons.from([])
        assert list.reduce() {value, element -> value + element} == null
        assert list.reduce(5) {value, element -> value + element} == 5
    }

    void testReduceNonEmtyList() {
        def list = Cons.from([1, 2, 3])
        assert list.reduce() {value, element -> value + element} == 6
        assert list.reduce(5) {value, element -> value + element} == 11
    }
}
