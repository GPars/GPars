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

package groovyx.gpars.dataflow

import spock.lang.Specification

class SelectToPromiseTest extends Specification {
    def "selecting from three df variables"() {
        given:
        def a = new DataflowVariable()
        def b = new DataflowVariable()
        def c = new DataflowVariable()

        def select = Dataflow.select(a, b, c)

        def p1 = select.selectToPromise()
        def p2 = select.selectToPromise()
        when:
        b << 10
        then:
        p1.get() == [1, 10] as SelectResult
        a << 30
        p2.get() == [0, 30] as SelectResult
    }

    def "selecting from three df streams"() {
        given:
        def a = new DataflowQueue()
        def b = new DataflowQueue()
        def c = new DataflowQueue()
        def select = Dataflow.select(a, b, c)

        def p1 = select.selectToPromise()
        def p2 = select.selectToPromise()
        when:
        b << 10

        then:
        p1.get() == [1, 10] as SelectResult
        a << 30
        p2.get() == [0, 30] as SelectResult
    }

    def "selecting from three df streams with a value being bound prior to selector creation"() {
        given:
        def a = new DataflowQueue()
        def b = new DataflowQueue()
        def c = new DataflowQueue()
        c << 20

        def select = Dataflow.select(a, b, c)

        when:
        def p = select.selectToPromise()

        then:
        p.get() == [2, 20] as SelectResult
    }

    def "selecting preserves order within a single stream"() {
        given:
        def a = new DataflowQueue()
        def b = new DataflowQueue()
        def c = new DataflowQueue()

        def select = Dataflow.select(a, b, c)

        when:
        b << 10
        b << 20
        def p1 = select.selectToPromise()
        def p2 = select.selectToPromise()
        def p3 = select.selectToPromise()
        b << 30

        then:
        p1.get() == [1, 10] as SelectResult
        p2.get() == [1, 20] as SelectResult
        p3.get() == [1, 30] as SelectResult
    }
}
