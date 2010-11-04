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

package groovyx.gpars.actor.blocking

import groovyx.gpars.actor.Actors

public class JoinTest extends GroovyTestCase {
    public void testActorJoin() {
        final def actor = Actors.actor { Thread.sleep 500; terminate()}
        actor.join()
        assertFalse actor.isActive()
    }

    public void testMultipleActorJoin() {
        final def actor1 = Actors.actor { Thread.sleep 500 }
        final def actor2 = Actors.actor { Thread.sleep 500 }
        [actor1, actor2]*.join()
        assertFalse actor1.isActive()
        assertFalse actor2.isActive()
    }

    public void testActorJoinWithoutTerminate() {
        final def actor = Actors.actor { Thread.sleep 500 }
        actor.join()
        assertFalse actor.isActive()
    }

    public void testCooperatingActorJoin() {
        final def actor1 = Actors.actor { react {} }
        final def actor2 = Actors.actor {actor1.join()}
        actor1 << 'Message'
        [actor1, actor2]*.join()
        assertFalse actor1.isActive()
        assertFalse actor2.isActive()
    }

    public void testStoppedActorJoin() {
        final def actor = Actors.actor { }
        actor.join()
        assertFalse actor.isActive()
        actor.join()
        assertFalse actor.isActive()
    }
}
