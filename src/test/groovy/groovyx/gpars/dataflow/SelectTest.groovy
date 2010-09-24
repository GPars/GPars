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

package groovyx.gpars.dataflow

import groovyx.gpars.actor.Actors
import groovyx.gpars.group.NonDaemonPGroup
import spock.lang.Specification

class SelectTest extends Specification {
    def "selecting from three df variables"() {
        given:
        def a = new DataFlowVariable()
        def b = new DataFlowVariable()
        def c = new DataFlowVariable()
        def select = DataFlow.select(a, b, c)
        when:
        b << 10
        then:
        select.val == 10
    }

    def "selecting from three df variables using custom item factory"() {
        given:
        def a = new DataFlowVariable()
        def b = new DataFlowVariable()
        def c = new DataFlowVariable()
        def select = DataFlow.select({item, index -> [value: item, index: index]}, a, b, c)
        when:
        b << 10
        then:
        select.val == [value: 10, index: 1]
    }

    def "selecting from three df variables with a value being bound prior to selector creation"() {
        given:
        def a = new DataFlowVariable()
        def b = new DataFlowVariable()
        def c = new DataFlowVariable()
        c << 20
        when:
        def select = DataFlow.select(a, b, c)
        then:
        select() == 20
    }

    def "selecting from three df streams"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def select = DataFlow.select(a, b, c)
        when:
        b << 10
        then:
        select.val == 10
    }

    def "selecting from three df streams using the select's output channel"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def select = DataFlow.select(a, b, c)
        when:
        b << 10
        then:
        select.outputChannel.val == 10
    }

    def "selecting from three df streams with a value being bound prior to selector creation"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        c << 20
        when:
        def select = DataFlow.select(a, b, c)
        then:
        select() == 20
    }

    def "selecting preserves order within a single stream"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def select = DataFlow.select(a, b, c)
        when:
        b << 10
        b << 20
        b << 30
        then:
        select.val == 10
        select.val == 20
        select.val == 30
    }

    def "selecting preserves order across streams"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def select = DataFlow.select(a, b, c)
        when:
        b << 10
        sleep 3000
        a << 20
        sleep 3000
        b << 30
        sleep 3000
        c << 40
        then:
        select.val == 10
        select.val == 20
        select.val == 30
        select.val == 40
    }

    def "closing a select will reject further requests"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def select = DataFlow.select(a, b, c)
        b << 10
        select.val
        c << 20
        select.close()
        when:
        select.val
        then:
        thrown(IllegalStateException)
    }

    def "closing a select will release the internal actor"() {
        given:
        def group = new NonDaemonPGroup()
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def select = group.select(a, b, c)
        b << 10
        select.val
        c << 20
        when:
        select.close()
        then:
        select.selector.actor.hasBeenStopped()
    }

    def "closing a fresh select will release the internal actor"() {
        given:
        def group = new NonDaemonPGroup()
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def select = group.select(a, b, c)
        when:
        select.close()
        then:
        select.selector.actor.hasBeenStopped()
    }

    def "selecting from three df streams using the getValAsync() method"() {
        given:
        def a = new DataFlowStream()
        def b = new DataFlowStream()
        def c = new DataFlowStream()
        def select = DataFlow.select(a, b, c)
        final DataFlowStream result = new DataFlowStream()

        def handler = Actors.actor {
            loop {
                result << receive()
            }
        }
        when:
        b << 10
        b << 20
        sleep 1000
        c << 30
        sleep 1000
        a << 40
        sleep 1000
        select.getValAsync handler
        select.getValAsync handler
        select.getValAsync handler
        select.getValAsync('attachment', handler)
        select.getValAsync handler
        c << 50
        select.outputChannel.getValAsync handler
        a << 60

        then:
        result.val == 10
        result.val == 20
        result.val == 30
        result.val == [attachment: 'attachment', result: 40]
        result.val == 50
        result.val == 60
        cleanup:
        handler.stop()
    }
}
