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

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.StaticDispatchActor
import groovyx.gpars.dataflow.Dataflows
import groovyx.gpars.group.DefaultPGroup

public class StaticDispatchActorTest extends GroovyTestCase {
    public void testDispatch() {
        TestStaticDispatchActor actor = new TestStaticDispatchActor<Integer>()
        actor.start()

        assert !actor.sendAndWait(1)
        assertFalse actor.stringFlag
        assert actor.integerFlag
        assert actor.objectFlag != null

        actor = new TestStaticDispatchActor<String>()
        actor.start()
        actor.sendAndWait ''
        assert actor.stringFlag
        assertFalse actor.integerFlag
        assert actor.objectFlag != null

        actor = new TestStaticDispatchActor<Object>()
        actor.start()
        actor.sendAndWait 1.0
        assertFalse actor.stringFlag
        assertFalse actor.integerFlag
        assert actor.objectFlag

        actor = new TestStaticDispatchActor()
        actor.start()
        actor.sendAndWait 3.0d
        assertFalse actor.stringFlag
        assertFalse actor.integerFlag
        assert actor.objectFlag

        actor = new TestStaticDispatchActor<List>()
        actor.start()
        actor.sendAndWait new ArrayList()
        assertFalse actor.stringFlag
        assertFalse actor.integerFlag
        assert actor.objectFlag != null
        assert actor.listFlag
    }

    public void testSendingList() {
        final Actor actor = new TestStaticDispatchActor<List>()
        actor.start()

        actor.sendAndWait(new ArrayList())
        assert actor.listFlag
    }

    public void testSendingSubclass() {
        TestStaticDispatchActor actor = new TestStaticDispatchActor<Number>()
        actor.start()

        assert !actor.sendAndWait(1)
        assertFalse actor.stringFlag
        assert actor.integerFlag
        assert actor.objectFlag != null
    }

    public void testSendingInvalidClass() {
        TestStaticDispatchActor actor = new TestStaticDispatchActor<Number>()
        actor.start()

        assert !actor.sendAndWait('Foo')
        assert actor.stringFlag
        assert !actor.integerFlag
        assert !actor.listFlag
        assert actor.objectFlag != null
    }

    public void testGroup() {
        final DefaultPGroup group = new DefaultPGroup()
        final StaticDispatchActor handler = group.staticMessageHandler {}
        assertSame group, handler.parallelGroup
        group.shutdown()
    }

    public void testGroupForFairActor() {
        final DefaultPGroup group = new DefaultPGroup()
        final StaticDispatchActor handler = group.fairStaticMessageHandler {}
        assertSame group, handler.parallelGroup
        group.shutdown()
    }

    public void testSendAndContinue() {
        def sda = Actors.staticMessageHandler {message ->
            reply 2 * message
        }

        final Dataflows results = new Dataflows()

        sda.sendAndContinue(1) {results.d1 = it}
        sda.sendAndContinue(2) {results.d2 = it}
        sda.sendAndContinue(3) {results.d3 = it}
        Actors.actor {
            sda.sendAndContinue(4) {results.d4 = it}
        }
        assert results.d1 == 2
        assert results.d2 == 4
        assert results.d3 == 6
        assert results.d4 == 8
    }
}

final class TestStaticDispatchActor<T> extends StaticDispatchActor<T> {
    volatile boolean stringFlag = false
    volatile boolean integerFlag = false
    volatile boolean listFlag = false
    volatile boolean objectFlag = false

    void onMessage(T message) {
        if (message instanceof Integer) integerFlag = true
        if (message instanceof String) stringFlag = true
        if (message instanceof List) listFlag = true
        if (message instanceof Object) objectFlag = true
        reply false
    }
}
