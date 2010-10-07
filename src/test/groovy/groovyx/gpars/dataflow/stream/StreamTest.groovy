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

import groovyx.gpars.dataflow.DataFlowVariable
import static groovyx.gpars.dataflow.DataFlow.task

class StreamTest extends GroovyTestCase {

    def stream = new Stream()

    void testEmptyStream() {
        task {
            stream << Stream.eos()
        }
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
            stream << "first" << "second" << "third" << Stream.eos();
        }
        assert stream.first == "first"
        assert stream.rest.first == "second"
        assert stream.rest.rest.first == "third"
        assert stream.rest.rest.rest.isEmpty()
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

    void testWriteDataFlowVariable() {
        def df = new DataFlowVariable()
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
            stream << "a" << "b" << "c" << Stream.eos()
        }
        def result = ""
        for (a in stream)
            result += a
        for (b in stream)
            result += b
        assert result == "abcabc"
    }

    void testGenerator() {
        stream.generate(1, {it + 1}, {it < 3})
        assert stream == new Stream({it << 1 << 2 << it.eos()})
    }

    void testGeneratorAsync() {
        def generator = {value ->
            task {value + 1}
        }
        stream.generate(1, generator, {it < 3})
        assert stream == new Stream({it << 1 << 2 << it.eos()})
    }

    void testApplyAsync() {
        task {
            stream.apply {it << 1 << 2 << it.eos()}
        }
        assert stream == new Stream({it << 1 << 2 << it.eos()})
    }

    void testMapWithIdentity() {
        task {
            stream << "a" << "b" << Stream.eos()
        }
        def mappedStream = stream.map {it}
        assert mappedStream == new Stream({it << "a" << "b" << it.eos()})
    }

    void testMapWithTransformation() {
        task {
            stream << "a" << "b" << Stream.eos()
        }
        def mappedStream = stream.map {it * 2}
        assert mappedStream == new Stream({it << "aa" << "bb" << it.eos()})
    }

    void testMapAsynchronously() {
        task {
            stream << "a" << "b" << Stream.eos()
        }
        def mappedStream = stream.map { value ->
            task {
                value * 2
            }
        }
        assert mappedStream == new Stream({it << "aa" << "bb" << it.eos()})
    }

    void testFilterNothing() {
        task {
            stream << 1 << 2 << 3 << 4 << Stream.eos()
        }
        def filteredStream = stream.filter { true }
        assert filteredStream == new Stream({it << 1 << 2 << 3 << 4 << it.eos()})
    }

    void testFilterEverything() {
        task {
            stream << 1 << 2 << 3 << 4 << Stream.eos()
        }
        def filteredStream = stream.filter { false }
        assert filteredStream.isEmpty()
    }

    void testFilterSomething() {
        task {
            stream << 1 << 2 << 3 << 4 << Stream.eos()
        }
        def filteredStream = stream.filter { it % 2 == 0 }
        assert filteredStream == new Stream({it << 2 << 4 << it.eos()})
    }

    void testFilterAsynchronously() {
        task {
            stream << 1 << 2 << 3 << 4 << Stream.eos()
        }
        def filteredStream = stream.filter { value ->
            task {value % 2 == 0}
        }
        assert filteredStream == new Stream({it << 2 << 4 << it.eos()})
    }

    void testReduceEmptyStream() {
        task {
            stream << Stream.eos()
        }
        assert stream.reduce() {value, element -> value + element} == null
        assert stream.reduce(5) {value, element -> value + element} == 5
    }

    void testReduceNonEmtyStream() {
        task {
            stream << 1 << 2 << 3 << Stream.eos()
        }
        assert stream.reduce() {value, element -> value + element} == 6
        assert stream.reduce(5) {value, element -> value + element} == 11
    }

    void testReduceAsynchronously() {
        task {
            stream.apply {s ->
                def i = 0
                while (i < 1000) {
                    s = s << i++
                }
                s << s.eos()
            }
        }
        def sum = stream.reduce() {value, element ->
            task {
                value + element
            }
        }
        assert sum == 499500
    }

}
