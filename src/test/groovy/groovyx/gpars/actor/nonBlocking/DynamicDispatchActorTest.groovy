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

package groovyx.gpars.actor.nonBlocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Dataflows
import groovyx.gpars.group.DefaultPGroup
import org.codehaus.groovy.runtime.NullObject

import java.util.concurrent.TimeUnit

public class DynamicDispatchActorTest extends GroovyTestCase {
    public void testDispatch() {
        final TestDynamicDispatchActor actor = new TestDynamicDispatchActor()
        actor.start()

        actor.sendAndWait 1
        assertFalse actor.stringFlag
        assert actor.integerFlag
        assertFalse actor.objectFlag
        actor.clearFlags();

        actor.sendAndWait ''
        assert actor.stringFlag
        assertFalse actor.integerFlag
        assertFalse actor.objectFlag
        actor.clearFlags();

        actor.sendAndWait 1.0
        assertFalse actor.stringFlag
        assertFalse actor.integerFlag
        assert actor.objectFlag
        actor.clearFlags();

        actor.sendAndWait new ArrayList()
        assertFalse actor.stringFlag
        assertFalse actor.integerFlag
        assertFalse actor.objectFlag
        assert actor.listFlag
        actor.clearFlags();

        actor.terminate()
    }

    public void testDispatchWithWhen() {
        boolean stringFlag = false
        boolean doubleFlag = false
        boolean objectFlag = false

        final Actor actor = new DynamicDispatchActor().become {
            when { String msg -> stringFlag = true; reply false }
            when { Double msg -> doubleFlag = true; reply false }
            when { msg -> objectFlag = true; reply false }
        }.start()

        actor.sendAndWait 1.0 as Double
        assertFalse stringFlag
        assert doubleFlag
        assertFalse objectFlag

        actor.sendAndWait ''
        assert stringFlag

        actor.sendAndWait new ArrayList()
        assert objectFlag

        actor.terminate()
    }

    public void testSendingList() {
        final Actor actor = new TestDynamicDispatchActor()
        actor.start()

        actor.sendAndWait(new ArrayList())
        assert actor.listFlag

        actor.terminate()
    }

    public void testSendingListViaWhen() {
        boolean flag = false

        final Actor actor = new DynamicDispatchActor().become {
            when { List msg -> flag = true; reply false }
        }
        actor.start()

        actor.sendAndWait(new ArrayList())
        assert flag

        actor.terminate()
    }

    public void testSendingSubclassViaWhen() {
        boolean numberFlag = false
        boolean doubleFlag = false

        final Actor actor = new DynamicDispatchActor().become {
            when { Number msg -> numberFlag = true; reply false }
            when { Double msg -> doubleFlag = true; reply false }
        }.start()

        actor.sendAndWait(1.0)
        assert numberFlag
        assertFalse doubleFlag
        numberFlag = false

        actor.sendAndWait(1.0 as Double)
        assertFalse numberFlag
        assert doubleFlag

        actor.terminate()
    }

    public void testDispatcher() {
        boolean stringFlag = false
        boolean integerFlag = false
        boolean objectFlag = false

        def actor = Actors.messageHandler {
            when { String message ->
                stringFlag = true
                reply false
            }

            when { Integer message ->
                integerFlag = true
                reply false
            }

            when { Object message ->
                objectFlag = true
                reply false
            }
        }

        actor.sendAndWait 1
        assertFalse stringFlag
        assert integerFlag
        assertFalse objectFlag

        actor.sendAndWait ''
        assert stringFlag
        assert integerFlag
        assertFalse objectFlag

        actor.sendAndWait 1.0
        assert stringFlag
        assert integerFlag
        assert objectFlag

        actor.sendAndWait new ArrayList()
        assert stringFlag
        assert integerFlag
        assert objectFlag

        actor.terminate()
    }

    public void testWhenAttachedAfterStart() {
        boolean stringFlag = false
        boolean integerFlag = false

        def dda = new DynamicDispatchActor().become { when { msg -> } }
        dda.when { String message ->
            stringFlag = true
            reply false
        }
        dda.start()

        dda.sendAndWait ''
        assert stringFlag
        assertFalse integerFlag

        dda.when { int message ->
            integerFlag = true
            reply false
        }

        dda.sendAndWait 1
        assert stringFlag
        dda.terminate()
    }

    public void testNullHandlerForSendWithNull() {
        boolean nullFlag = false

        def dda = new DynamicDispatchActor()
        dda.when { NullObject message ->
            nullFlag = true
            reply false
        }
        dda.start()

        dda.sendAndWait(null)
        assert nullFlag
        dda.terminate()
    }

    public void testClosureMessage() {
        boolean flag = false

        def dda = new DynamicDispatchActor()
        dda.when { Closure cl ->
            reply cl()
        }
        dda.start()

        dda.sendAndWait { flag = true }
        assert flag
        dda.terminate()
    }

    public void testGroup() {
        final DefaultPGroup group = new DefaultPGroup()
        final DynamicDispatchActor handler = group.messageHandler {}
        assertSame group, handler.parallelGroup
        group.shutdown()
    }

    public void testReplies() {
        def dda = Actors.messageHandler {
            when { message ->
                reply 10
                sender.send 20
            }
        }

        def results = new DataflowVariable()

        Actors.actor {
            dda << 1
            react(1000, TimeUnit.MILLISECONDS) { a ->
                react(1000, TimeUnit.MILLISECONDS) { b ->
                    results << [a, b]
                }
            }
            dda.stop()
        }
        assert results.val == [10, 20]

        dda.terminate()
    }

    public void testSendAndWait() {
        def dda = Actors.messageHandler {
            when { message ->
                reply 10
            }
        }

        Actors.actor {
            assert 10 == dda.sendAndWait(1)
            assert 10 == dda.sendAndWait(1)
            dda.terminate()
        }
    }

    public void testSendAndContinue() {
        def dda = Actors.messageHandler {
            when { message ->
                reply 2 * message
            }
        }

        final Dataflows results = new Dataflows()

        dda.sendAndContinue(1) { results.d1 = it }
        dda.sendAndContinue(2) { results.d2 = it }
        dda.sendAndContinue(3) { results.d3 = it }
        Actors.actor {
            dda.sendAndContinue(4) { results.d4 = it }
        }
        assert results.d1 == 2
        assert results.d2 == 4
        assert results.d3 == 6
        assert results.d4 == 8

        dda.terminate()
    }

    public void testWhenInConstructor() {

        final def actor = new MyActor({
            when { BigDecimal num -> results << 'BigDecimal' }
            when { Double num -> results << 'Double' }
        }).start()

        actor 1
        actor ''
        actor 1.0
        actor([1, 2, 3, 4, 5])

        actor.join()
        assert 'Integer' == actor.results.val
        assert 'string' == actor.results.val
        assert 'BigDecimal' == actor.results.val
        assert 'list' == actor.results.val

        actor.terminate()
    }

    public void testWhenInBecome() {

        final def actor = new MyActor().become {
            when { BigDecimal num -> results << 'BigDecimal' }
            when { Double num -> results << 'Double' }
        }.start()

        actor 1
        actor ''
        actor 1.0
        actor([1, 2, 3, 4, 5])

        actor.join()
        assert 'Integer' == actor.results.val
        assert 'string' == actor.results.val
        assert 'BigDecimal' == actor.results.val
        assert 'list' == actor.results.val

        actor.terminate()
    }

    public void testWhenOverOnMessage() {

        final def actor = new MyActor().start()

        actor 1
        actor ''

        assert 'Integer' == actor.results.val
        assert 'string' == actor.results.val

        actor.when { Integer num -> results << 'Integer2' }
        actor 1
        assert 'Integer2' == actor.results.val
        actor.when { Integer num -> results << 'Integer3' }
        actor 1
        assert 'Integer3' == actor.results.val
        actor.stop()
        actor.join()

        actor.terminate()
    }

    public void testWhenOverMoreGenericOnMessage() {

        final def actor = new MyGenericActor().start()

        actor 1
        actor ''

        assert 'Object' == actor.results.val
        assert 'string' == actor.results.val

        actor.when { Integer num -> results << 'Integer2' }
        actor 1
        assert 'Integer2' == actor.results.val
        actor.when { Integer num -> results << 'Integer3' }
        actor 1
        assert 'Integer3' == actor.results.val
        actor.stop()
        actor.join()

        actor.terminate()
    }

    public void testWhenOverWhen() {

        final def actor = new MyActor()
        actor.metaClass.onMessage { BigDecimal num -> results << 'BigDecimal' }
        actor.metaClass.onMessage { Double num -> results << 'Double' }
        actor.start()

        actor 1
        actor ''
        actor 1.0G

        assert 'Integer' == actor.results.val
        assert 'string' == actor.results.val
        assert 'BigDecimal' == actor.results.val

        actor.metaClass.onMessage { BigDecimal num -> results << 'BigDecimal2' }
        actor 1.0G
        assert 'BigDecimal2' == actor.results.val
        actor.stop()
        actor.join()

    }
}

final class MyActor extends DynamicDispatchActor {

    def results = new DataflowQueue()

    def MyActor() {}

    def MyActor(final closure) {
        super()
        become(closure)
    }

    void onMessage(String message) { results << 'string' }

    void onMessage(Integer message) { results << 'Integer' }

    void onMessage(List message) { results << 'list'; stop() }
}

final class MyGenericActor extends DynamicDispatchActor {

    def results = new DataflowQueue()

    def MyActor() {}

    void onMessage(String message) { results << 'string' }

    void onMessage(Object message) { results << 'Object' }
}

final class TestDynamicDispatchActor extends DynamicDispatchActor {
    volatile boolean stringFlag = false
    volatile boolean integerFlag = false
    volatile boolean listFlag = false
    volatile boolean objectFlag = false

    def clearFlags() {
        stringFlag = false;
        integerFlag = false;
        listFlag = false;
        objectFlag = false;
    }

    TestDynamicDispatchActor() {
        when { String message ->
            stringFlag = true
            reply false
        }
    }

    void onMessage(Integer message) {
        integerFlag = true
        reply false
    }

    void onMessage(Object message) {
        objectFlag = true
        reply false
    }

    void onMessage(List message) {
        listFlag = true
        reply false
    }
}
