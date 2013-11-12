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

package groovyx.gpars.dataflow

import spock.lang.Specification

import static groovyx.gpars.dataflow.Dataflow.task

class ForkAndJoinPromiseTest extends Specification {
    def "basic for and join returns a list of individual results"() {
        given:
        def a = new DataflowVariable()
        def b = new DataflowVariable()
        def c = new DataflowVariable()
        def d = new DataflowVariable()

        when:
        def result = task {
            a << 1
        }.thenForkAndJoin({ b << 2; 2 }, { c << 3; 3 }).then { d << it; it }.get()

        then:
        result == [2, 3]
        a.val == 1
        b.val == 2
        c.val == 3
        d.val == [2, 3]
    }

    def "an exception gets propagated to the result"() {
        given:
        def a = new DataflowVariable()
        def b = new DataflowVariable()
        def c = new DataflowVariable()
        def d = new DataflowVariable()

        when:
        Promise result = task {
            a << 1
        }.thenForkAndJoin({ b << 2; 2 }, { c << 3; 3 }, { throw new RuntimeException('test') }).then { d << it; it }
        result.join()

        then:
        result.isError()
        a.val == 1
        b.val == 2
        c.val == 3
        !d.bound
    }

    def "an exception gets caught by an error handler"() {
        given:
        def a = new DataflowVariable()
        def b = new DataflowVariable()
        def c = new DataflowVariable()
        def d = new DataflowVariable()

        when:
        Promise result = task {
            a << 1
        }.thenForkAndJoin({ b << 2; 2 }, { c << 3; 3 }, { throw new RuntimeException('test') }).then({ d << it; it }, { "error caught" })
        result.join()

        then:
        result.get() == "error caught"
        a.val == 1
        b.val == 2
        c.val == 3
        !d.bound
    }

}
