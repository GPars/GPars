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

package groovyx.gpars.dataflow

import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.ResizeablePool
import java.util.concurrent.atomic.AtomicInteger
import spock.lang.Specification
import static groovyx.gpars.dataflow.ProcessingNode.node

class KanbanFlowTest extends Specification {

    AtomicInteger count   = new AtomicInteger(0) // needed to create some products
    DataflowQueue results = new DataflowQueue()  // needed to assert collected products
    KanbanFlow    flow    = new KanbanFlow()

    def counter   = { below -> below count.andIncrement }
    def reporter  = { above -> results << above.take() }
    def repeater  = { above, below -> below above.take() }
    def increment = { above, below -> below << above.take() + 1 }

    def "A -> B wire simple flow with one producer and one consumer"() {
        given:
        def producer = node(counter)
        def consumer = node(reporter)
        flow.link(producer).to(consumer)

        when: flow.start(1)
        then:
        results.val == 0
        results.val == 1
        flow.stop()
    }

    def "A -> B wire simple flow with one producer and one consumer with a custom pooled group"() {
        given:
        def producer = node(counter)
        def consumer = node(reporter)
        flow.pooledGroup = new DefaultPGroup(new ResizeablePool(true, 1))
        flow.link(producer).to(consumer)

        when: flow.start(1)
        then:
        results.val == 0
        results.val == 1
        flow.stop()
    }

    def "A -> B -> C flow with chain of three processing units"() {
        given:
        def producer = node(counter)
        def middle   = node(repeater)
        def consumer = node(reporter)

        flow.with {
            link(producer).to(middle)
            link(middle).to(consumer)
        }

        when: flow.start(1)

        then:
        results.val == 0
        results.val == 1
        flow.stop()
    }

    def "A* -> B* simple flow with many producer and consumers forks"() {
        given:
        def producer = node(counter);  producer.maxForks = producerForks
        def consumer = node(reporter); consumer.maxForks = consumerForks
        flow.link(producer).to(consumer)

        when: flow.start()      // uses optimal number of trays
        then:
        def forks = consumerForks + producerForks
        results.val < forks     // "worst" case of scheduling
        results.val < forks * 2
        flow.stop()

        where:
        producerForks | consumerForks
        1             | 1
        1             | 2
        1             | 3
        2             | 1
        2             | 2
        2             | 3
        3             | 1
        3             | 2
        3             | 3
    }

    def "A -> B,C flow with one producer and two consumers(broadcast)"() {
        given:
        def producer = node { down1, down2 ->
            def val = count.andIncrement
            down1 << val
            down2 << val
        }
        def consumer1 = node(reporter)
        def consumer2 = node(reporter)
        flow.link(producer).to(consumer1)
        flow.link(producer).to(consumer2)

        when: flow.start()
        then:
        4.times { results.val < 4 } // "worst" case of scheduling when one consumer is always faster
        flow.stop()
    }

    def "A -> B,C flow with one producer and two consumers(master-slave)"() {
        given:
        def producer  = node {down1, down2 -> counter down1; counter down2 }
        def consumer1 = node(reporter)
        def consumer2 = node(reporter)
        flow.link(producer).to(consumer1)
        flow.link(producer).to(consumer2)

        when: flow.start(1)
        then:
        def reported = []
        4.times { reported << results.val }
        reported.each { it < 5 } // "worst" case of scheduling when one consumer is always faster
        flow.stop()
    }

    def "A -> B,C flow with one producer and two consumers(selector)"() {
        given:
        def producer  = node {down1, down2 -> counter down1; ~down2 }  // simple selection: always choose first
        def consumer1 = node(reporter)
        def consumer2 = node(reporter)
        flow.link(producer).to(consumer1)
        flow.link(producer).to(consumer2)

        when: flow.start(1)
        then:
        def reported = []
        4.times { reported << results.val }
        reported == [0, 1, 2, 3]
        flow.stop()
    }

    def "A,B -> C flow with two producers and one consumer(collector)"() {
        given:
        def producer1 = node(counter)
        def producer2 = node(counter)
        def consumer  = node { a, b -> reporter a; reporter b }
        flow.link(producer1).to(consumer)
        flow.link(producer2).to(consumer)

        when: flow.start(1)
        then:
        def reported = []
        4.times { reported << results.val }
        reported.containsAll([0, 1, 2, 3])
        flow.stop()
    }

    def "A -> B,C -> D diamond"() {
        given:
        def producer = node {down1, down2 -> counter down1; counter down2 }
        def middle1  = node(increment)
        def middle2  = node(increment)
        def collector = node { a, b -> reporter a; reporter b }

        flow.with {
            link(producer).to(middle1)
            link(producer).to(middle2)
            link(middle1).to(collector)
            link(middle2).to(collector)
        }

        when: flow.start(1)
        then:
        def reported = []
        4.times { reported << results.val }
        reported[0..1].containsAll([1, 2])
        reported[2..3].containsAll([3, 4])
        flow.stop()
    }

    def "A <-> B cycle: is disallowed by default"() {
        given:
        def a = node {}
        def b = node {}

        when:
        flow.with {
            link(a).to(b)
            link(b).to(a)
        }
        then:
        thrown IllegalArgumentException
    }

    def "A -> B -> C -> A cycle: is disallowed by default"() {
        given:
        def a = node {}
        def b = node {}
        def c = node {}

        when:
        flow.with {
            link(a).to(b)
            link(b).to(c)
            link(c).to(a)
        }
        then:
        thrown IllegalArgumentException
    }

    def "A <-> A cycle: is disallowed by default"() {
        given:
        def a = node {}
        when:
        flow.link(a).to(a)
        then:
        thrown IllegalArgumentException
    }

    def "A <-> A cycle: ping-ping self-contained counter (generator)"() {
        given:
        def a = node { up, dn -> def count = up.take() + 1; results << count; dn << count }

        flow.cycleAllowed = true
        KanbanLink link
        link = flow.link(a).to(a)

        when: flow.start(1)
        link.downstream << new KanbanTray(link: link, product: 0)

        then:
        def reported = []
        4.times { reported << results.val }
        reported.containsAll([1, 2, 3, 4])
        flow.stop()
    }

    def "A <-> A, A -> B generator heartbeat"() {
        given:
        def heartbeat = node { fromSelf, toSelf, toTheRest ->
            def prod = fromSelf.take() + 1
            toSelf    prod
            toTheRest prod
        }
        def consumer = node(reporter)

        flow.cycleAllowed = true
        KanbanLink lazySequence = flow.link(heartbeat).to(heartbeat)
        flow.link(heartbeat).to(consumer)

        when: flow.start()
        lazySequence.downstream << new KanbanTray(link: lazySequence, product: 0)

        then:
        def reported = []
        4.times { reported << results.val }
        reported == [1, 2, 3, 4]
        flow.stop()
    }

    def "A <-> B cycle: ping-pong self-contained counter (generator)"() {
        given:
        def a = node { dn, up -> def val = up.take(); results << val; dn << val }
        def b = node(increment)

        flow.cycleAllowed = true
        KanbanLink up
        flow.with {
            link(a).to(b)
            up = link(b).to(a)
        }

        when: flow.start(1)
        up.downstream << new KanbanTray(link: up, product: 0)

        then:
        def reported = []
        4.times { reported << results.val }
        reported.containsAll([0, 1, 2, 3])
        flow.stop()
    }

    def "A -> B -> C -> A cycle: ping-peng-pong chained counter (generator)"() {
        given:
        def a = node { dn, homeTray ->  dn << homeTray.take()  }
        def b = node(increment)
        def c = node { up, homeTray -> def val = up.product; reporter up; homeTray << val }

        flow.cycleAllowed = true
        KanbanLink home
        flow.with {
            link(a).to(b)
            link(b).to(c)
            home = link(c).to(a)
        }

        when: flow.start(1)
        home.downstream << new KanbanTray(link: home, product: 0)

        then:
        def reported = []
        4.times { reported << results.val }
        reported.containsAll([1, 2, 3, 4])
        flow.stop()
    }

    def "flow composition with single append"() {
        given:
        def firstFlow = new KanbanFlow()
        def producer  = node(counter)
        def consumer  = node(repeater)
        firstFlow.link(producer).to(consumer)

        def secondFlow = new KanbanFlow()
        def producer2  = node(repeater)
        def consumer2  = node(reporter)
        secondFlow.link(producer2).to(consumer2)

        flow = firstFlow + secondFlow

        when: flow.start(1)
        then:
        results.val == 0
        results.val == 1
        flow.stop()
    }
}
