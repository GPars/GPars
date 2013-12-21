// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
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

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Dataflows

import static groovyx.gpars.dataflow.Dataflow.task

@SuppressWarnings("SpellCheckingInspection")
class SyncDataflowStreamDefaultTest extends GroovyTestCase {

    def stream = new SyncDataflowStream(0)

    void testEmptyStream() {
        stream << SyncDataflowStream.eos()
        assert stream.isEmpty()
    }

    void testStreamWithOneObject() {
        task {
            stream << "first"
        }
        assert stream.first == "first"
        assert !stream.isEmpty()
    }

    void testStreamWithSeveralObjects() {
        task {
            stream << "first" << "second" << "third" << SyncDataflowStream.eos();
        }
        assert stream.first == "first"
        assert stream.rest.first == "second"
        assert stream.rest.rest.first == "third"
        assert stream.rest.rest.rest.isEmpty()
    }

    void testStreamReadFromManyConsumers() {
        def results = new Dataflows()
        def numConsumers = 10
        task {
            stream << "a" << "b" << SyncDataflowStream.eos();
        }
        (1..numConsumers).each { index ->
            task {
                results[index] = (stream.first + stream.rest.first)
            }
        }
        (1..numConsumers).each { index ->
            assert results[index] == "ab"
        }
    }

    void testWritingIncompatiblyTwiceThrowsException() {
        task {
            stream << "first"
        }
        stream << "first" // should work
        shouldFail(IllegalStateException) {
            stream << "second"
        }
    }

    void testWriteDataflowVariable() {
        def df = new DataflowVariable()
        task {
            stream << df
        }
        task {
            df << "first"
        }
        assert stream.first == "first"
    }

    void testIteratingAStream() {
        task {
            stream << "a" << "b" << "c" << SyncDataflowStream.eos()
        }
        def result = ""
        for (a in stream)
            result += a
        for (b in stream)
            result += b
        assert result == "abcabc"
    }

    void testGenerator() {
        stream.generate(1, { it + 1 }, { it < 3 })
        assert stream == new SyncDataflowStream(0, { it << 1 << 2 << it.eos() })
    }

    void testGeneratorAsync() {
        def generator = { value ->
            task { value + 1 }
        }
        stream.generate(1, generator, { it < 3 })
        assert stream == new SyncDataflowStream(0, { it << 1 << 2 << it.eos() })
    }

    void testApplyAsync() {
        task {
            stream.apply { it << 1 << 2 << it.eos() }
        }
        assert stream == new SyncDataflowStream(0, { it << 1 << 2 << it.eos() })
    }

    void testMapWithIdentity() {
        task {
            stream << "a" << "b" << SyncDataflowStream.eos()
        }
        def mappedStream = stream.map { it }
        assert mappedStream == new SyncDataflowStream(0, { it << "a" << "b" << it.eos() })
    }

    void testMapWithTransformation() {
        task {
            stream << "a" << "b" << SyncDataflowStream.eos()
        }
        def mappedStream = stream.map { it * 2 }
        assert mappedStream == new SyncDataflowStream(0, { it << "aa" << "bb" << it.eos() })
    }

    void testMapAsynchronously() {
        task {
            stream << "a" << "b" << SyncDataflowStream.eos()
        }
        def mappedStream = stream.map { value ->
            task {
                value * 2
            }
        }
        assert mappedStream == new SyncDataflowStream(0, { it << "aa" << "bb" << it.eos() })
    }

    void testFilterNothing() {
        task {
            stream << 1 << 2 << 3 << 4 << SyncDataflowStream.eos()
        }
        def filteredStream = stream.filter { true }
        assert filteredStream == new SyncDataflowStream(0, { it << 1 << 2 << 3 << 4 << it.eos() })
    }

    void testFilterEverything() {
        task {
            stream << 1 << 2 << 3 << 4 << SyncDataflowStream.eos()
        }
        def filteredStream = stream.filter { false }
        assert filteredStream.isEmpty()
    }

    void testFilterSomething() {
        task {
            stream << 1 << 2 << 3 << 4 << SyncDataflowStream.eos()
        }
        def filteredStream = stream.filter { it % 2 == 0 }
        assert filteredStream == new SyncDataflowStream(0, { it << 2 << 4 << it.eos() })
    }

    void testFilterAsynchronously() {
        task {
            stream << 1 << 2 << 3 << 4 << SyncDataflowStream.eos()
        }
        def filteredStream = stream.filter { value ->
            task { value % 2 == 0 }
        }
        assert filteredStream == new SyncDataflowStream(0, { it << 2 << 4 << it.eos() })
    }

    void testReduceEmptyStream() {
        task {
            stream << SyncDataflowStream.eos()
        }
        assert stream.reduce() { value, element -> value + element } == null
        assert stream.reduce(5) { value, element -> value + element } == 5
    }

    void testReduceNonEmptyStream() {
        task {
            stream << 1 << 2 << 3 << SyncDataflowStream.eos()
        }
        assert stream.reduce() { value, element -> value + element } == 6
        assert stream.reduce(5) { value, element -> value + element } == 11
    }

    void testReduceAsynchronously() {
        task {
            stream.apply { s ->
                def i = 0
                while (i < 1000) {
                    s = s << i++
                }
                s << s.eos()
            }
        }
        def sum = stream.reduce() { value, element ->
            task {
                value + element
            }
        }
        assert sum == 499500
    }

    void testLargeStream() {
        int n = 10000 // stack overflow with tail recursion
        //int n = 1000 // works with tail recursion
        def expectedSum = (n * (n - 1)) / 2

        stream.generate(1, { it + 1 }, { it < n })

        int sumFromIteration = 0
        for (a in stream)
            sumFromIteration += a
        assert sumFromIteration == expectedSum

        def sumFromReduce = stream.reduce() { value, element ->
            task {
                value + element
            }
        }
        assert sumFromReduce == expectedSum

        def streamPlusOne = stream.map { value -> value + 1 }
        assert streamPlusOne.first == 2

        def streamOnlyOdd = stream.filter { value -> (value % 2) == 1 }
        assert streamOnlyOdd.rest.first == 3
    }

}