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

package org.gparallelizer.actors.groups

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actors
import org.gparallelizer.actors.AbstractThreadActorGroup
import org.gparallelizer.actors.AbstractThreadActor
import org.gparallelizer.actors.ActorMessage
import java.util.concurrent.LinkedBlockingQueue
import org.gparallelizer.actors.ThreadActorGroup
import org.gparallelizer.actors.NonDaemonThreadActorGroup
import org.gparallelizer.actors.AbstractThreadActorGroup
import org.gparallelizer.actors.AbstractThreadActor
import org.gparallelizer.actors.NonDaemonThreadActorGroup
import org.gparallelizer.actors.NonDaemonThreadActorGroup

public class ActorGroupTest extends GroovyTestCase {
    public void testDefaultGroupDaemon() {
        volatile boolean daemon = false;
        final CountDownLatch latch = new CountDownLatch(1)

        def actor = Actors.oneShotActor {
            daemon = Thread.currentThread().isDaemon()
            latch.countDown()
        }.start()

        assertEquals Actors.defaultActorGroup, actor.actorGroup
        latch.await()
        assert daemon
    }

    public void testGroupDaemonFlag() {
        volatile boolean daemon = false;
        final CountDownLatch latch1 = new CountDownLatch(1)
        final CountDownLatch latch2 = new CountDownLatch(1)

        final AbstractThreadActorGroup daemonGroup = new ThreadActorGroup()
        final AbstractThreadActorGroup nonDaemonGroup = new NonDaemonThreadActorGroup()

        def actor1 = daemonGroup.oneShotActor {
            daemon = Thread.currentThread().isDaemon()
            latch1.countDown()
        }.start()

        assertEquals daemonGroup, actor1.actorGroup
        latch1.await()
        assert daemon

        def actor2 = nonDaemonGroup.oneShotActor {
            daemon = Thread.currentThread().isDaemon()
            latch2.countDown()
        }.start()

        assertEquals nonDaemonGroup, actor2.actorGroup
        latch2.await()
        assertFalse daemon
    }

    public void testGroupsWithActorInheritance() {
        volatile boolean daemon = false;
        final CountDownLatch latch1 = new CountDownLatch(1)
        final CountDownLatch latch2 = new CountDownLatch(1)

        final AbstractThreadActorGroup daemonGroup = new ThreadActorGroup()
        final AbstractThreadActorGroup nonDaemonGroup = new NonDaemonThreadActorGroup()

        final GroupTestActor actor1 = new GroupTestActor(daemonGroup)
        actor1.metaClass.act = {->
            daemon = Thread.currentThread().isDaemon()
            latch1.countDown()
            stop()
        }
        actor1.start()

        assertEquals daemonGroup, actor1.actorGroup
        latch1.await()
        assert daemon

        final GroupTestActor actor2 = new GroupTestActor(nonDaemonGroup)
        actor2.metaClass.act = {->
            daemon = Thread.currentThread().isDaemon()
            latch2.countDown()
            stop()
        }
        actor2.start()

        assertEquals nonDaemonGroup, actor2.actorGroup
        latch2.await()
        assertFalse daemon
    }

    public void testValidGroupReset() {
        final AbstractThreadActorGroup daemonGroup = new ThreadActorGroup()
        final AbstractThreadActorGroup nonDaemonGroup = new NonDaemonThreadActorGroup()
        final GroupTestActor actor = new GroupTestActor(daemonGroup)

        assertEquals daemonGroup, actor.actorGroup
        actor.actorGroup = nonDaemonGroup
        assertEquals nonDaemonGroup, actor.actorGroup

    }

    public void testInvalidGroupReset() {
        final AbstractThreadActorGroup daemonGroup = new ThreadActorGroup()
        final AbstractThreadActorGroup nonDaemonGroup = new NonDaemonThreadActorGroup()
        final GroupTestActor actor = new GroupTestActor(daemonGroup)
        actor.start()
        assertEquals daemonGroup, actor.actorGroup
        shouldFail {
            actor.actorGroup = nonDaemonGroup
        }
    }
}

class GroupTestActor extends AbstractThreadActor {

    def GroupTestActor(AbstractThreadActorGroup group) {
        super(new LinkedBlockingQueue<ActorMessage>());
        actorGroup = group
    }

    protected void act() {
    }
}
