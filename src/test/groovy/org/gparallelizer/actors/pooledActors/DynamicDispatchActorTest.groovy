//  GParallelizer
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

package org.gparallelizer.actors.pooledActors

public class DynamicDispatchActorTest extends GroovyTestCase {
    public void testDispatch() {
        final TestDynamicDispatchActor actor = new TestDynamicDispatchActor().start()

        actor.sendAndWait 1
        assertFalse actor.stringFlag
        assert actor.integerFlag
        assertFalse actor.objectFlag

        actor.sendAndWait ''
        assert actor.stringFlag
        assert actor.integerFlag
        assertFalse actor.objectFlag

        actor.sendAndWait 1.0
        assert actor.stringFlag
        assert actor.integerFlag
        assert actor.objectFlag

        actor.sendAndWait new ArrayList()
        assert actor.stringFlag
        assert actor.integerFlag
        assert actor.objectFlag
    }
}

final class TestDynamicDispatchActor extends DynamicDispatchActor {
    volatile boolean stringFlag = false
    volatile boolean integerFlag = false
    volatile boolean objectFlag = false

    void onMessage(String message) {
        stringFlag = true
        reply false
    }

    void onMessage(Integer message) {
        integerFlag = true
        reply false
    }

    void onMessage(Object message) {
        objectFlag = true
        reply false
    }
}
