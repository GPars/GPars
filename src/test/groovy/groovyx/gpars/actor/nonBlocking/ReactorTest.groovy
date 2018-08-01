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

package groovyx.gpars.actor.nonBlocking

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.ReactiveActor
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Dataflows
import groovyx.gpars.group.DefaultPGroup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

public class ReactorTest extends GroovyTestCase {

    public void testSimple() {
        def res = []
        CountDownLatch latch = new CountDownLatch(6)
        final def processor = Actors.reactor {
            res << 2 * it
            latch.countDown()
        }

        (0..5).each {
            processor << it
        }

        latch.await()
        processor.stop()
        processor.join()

        assert [0, 2, 4, 6, 8, 10] == res
    }

    public void testWait() {
        final def processor = Actors.reactor {
            2 * it
        }

        assert 20 == processor.sendAndWait(10)
        assert 40 == processor.sendAndWait(20)
        assert 60 == processor.sendAndWait(30)

        processor.stop()
        processor.join(10, TimeUnit.SECONDS)
    }

    public void testMessageProcessing() {
        def group = new DefaultPGroup(4)
        def result1 = new AtomicInteger(0)
        def result2 = new AtomicInteger(0)
        def result3 = new AtomicInteger(0)

        final def processor = group.reactor {
            2 * it
        }

        final def a1 = group.actor {
            result1 = processor.sendAndWait(10)
        }

        final def a2 = group.actor {
            result2 = processor.sendAndWait(20)
        }

        final def a3 = group.actor {
            result3 = processor.sendAndWait(30)
        }

        [a1, a2, a3]*.join()
        assert 20 == result1
        assert 40 == result2
        assert 60 == result3

        processor.stop()
        processor.join()
        group.shutdown()
    }

    public void testGroup() {
        final DefaultPGroup group = new DefaultPGroup()
        final ReactiveActor reactor = group.reactor {}
        assertSame group, reactor.parallelGroup
        group.shutdown()
    }

    public void testNullMessage() {
        def res = []
        CountDownLatch latch = new CountDownLatch(1)
        final def processor = Actors.reactor {
            res << it
            latch.countDown()
        }

        processor << null

        latch.await()
        processor.stop()
        processor.join()

        assert [null] == res
    }

    public void testReplies() {
        def reactor = Actors.reactor {
            reply 10
            sender.send 20
            30
        }

        def results = new DataflowVariable()

        Actors.blockingActor {
            reactor << 1
            results << (1..3).collect {receive(1000, TimeUnit.MILLISECONDS)}
            reactor.stop()
        }
        assert results.val == [10, 20, 30]
    }

    public void testSendAndWait() {
        def reactor = Actors.reactor {
            10
        }

        Actors.actor {
            assert 10 == reactor.sendAndWait(1)
            assert 10 == reactor.sendAndWait(1)
        }
    }

    public void testSendAndContinue() {
        def reactor = Actors.reactor {
            2 * it
        }

        final Dataflows results = new Dataflows()

        reactor.sendAndContinue(1) {results.d1 = it}
        reactor.sendAndContinue(2) {results.d2 = it}
        reactor.sendAndContinue(3) {results.d3 = it}
        Actors.actor {
            reactor.sendAndContinue(4) {results.d4 = it}
        }
        assert results.d1 == 2
        assert results.d2 == 4
        assert results.d3 == 6
        assert results.d4 == 8
    }
}
