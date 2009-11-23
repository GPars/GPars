//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package groovyx.gpars.actor.nonBlocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.actor.PooledActorGroup
import org.codehaus.groovy.runtime.NullObject

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
    }

    public void testDispatchWithWhen() {
        volatile boolean stringFlag = false
        volatile boolean doubleFlag = false
        volatile boolean objectFlag = false

        final Actor actor = new DynamicDispatchActor({
            when {String msg -> stringFlag = true; reply false}
            when {Double msg -> doubleFlag = true; reply false}
            when {msg -> objectFlag = true; reply false}
        })
        actor.start()

        actor.sendAndWait 1.0 as Double
        assertFalse stringFlag
        assert doubleFlag
        assertFalse objectFlag

        actor.sendAndWait ''
        assert stringFlag

        actor.sendAndWait new ArrayList()
        assert objectFlag
    }

    public void testSendingList() {
        final Actor actor = new TestDynamicDispatchActor()
        actor.start()

        actor.sendAndWait(new ArrayList())
        assert actor.listFlag
    }

    public void testSendingListViaWhen() {
        volatile boolean flag = false

        final Actor actor = new DynamicDispatchActor({
            when {List msg -> flag = true; reply false}
        })
        actor.start()

        actor.sendAndWait(new ArrayList())
        assert flag
    }

    public void testSendingSubclassViaWhen() {
        volatile boolean numberFlag = false
        volatile boolean doubleFlag = false

        final Actor actor = new DynamicDispatchActor({
            when {Number msg -> numberFlag = true; reply false}
            when {Double msg -> doubleFlag = true; reply false}
        })
        actor.start()

        actor.sendAndWait(1.0)
        assert numberFlag
        assertFalse doubleFlag
        numberFlag = false

        actor.sendAndWait(1.0 as Double)
        assertFalse numberFlag
        assert doubleFlag

    }

    public void testDispatcher() {
        volatile boolean stringFlag = false
        volatile boolean integerFlag = false
        volatile boolean objectFlag = false

        def actor = Actors.messageHandler {
            when {String message ->
                stringFlag = true
                reply false
            }

            when {Integer message ->
                integerFlag = true
                reply false
            }

            when {Object message ->
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
    }

    public void testWhenAttachedAfterCtor() {
        volatile boolean stringFlag = false
        volatile boolean integerFlag = false

        def dda = new DynamicDispatchActor()
        dda.when {String message ->
            stringFlag = true
            reply false
        }
        dda.start()

        dda.sendAndWait ''
        assert stringFlag
        assertFalse integerFlag

        dda.when {int message ->
            integerFlag = true
            reply false
        }

        dda.sendAndWait 1
        assert stringFlag
    }

    public void testNullHandlerForSendWithNull() {
        volatile boolean nullFlag = false

        def dda = new DynamicDispatchActor()
        dda.when {NullObject message ->
            nullFlag = true
            reply false
        }
        dda.start()

        dda.sendAndWait(null)
        assert nullFlag
    }

    public void testClosureMessage() {
        volatile boolean flag = false

        def dda = new DynamicDispatchActor()
        dda.when {Closure cl ->
            reply cl()
        }
        dda.start()

        dda.sendAndWait { flag = true }
        assert flag
    }

    public void testGroup() {
        final PooledActorGroup group = new PooledActorGroup()
        final DynamicDispatchActor handler = group.messageHandler {}
        assertSame group, handler.actorGroup
    }
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
        when {String message ->
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
