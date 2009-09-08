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

package org.gparallelizer.actors.pooledActors.groups

import org.gparallelizer.actors.pooledActors.PooledActors
import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.NonDaemonPooledActorGroup
import org.gparallelizer.actors.AbstractActorGroup

public class PooledActorGroupTest extends GroovyTestCase {

    public void testDefaultGroupDaemon() {
        volatile boolean daemon = false;
        final CountDownLatch latch = new CountDownLatch(1)

        def actor = PooledActors.actor {
            daemon = Thread.currentThread().isDaemon()
            latch.countDown()
        }.start()

        assertEquals PooledActors.defaultPooledActorGroup, actor.actorGroup
        latch.await()
        assert daemon
    }

    public void testGroupDaemonFlag() {
        volatile boolean daemon = false;
        final CountDownLatch latch1 = new CountDownLatch(1)
        final CountDownLatch latch2 = new CountDownLatch(1)

        final PooledActorGroup daemonGroup = new PooledActorGroup(false)
        final AbstractActorGroup nonDaemonGroup = new NonDaemonPooledActorGroup()

        def actor1 = daemonGroup.actor {
            daemon = Thread.currentThread().isDaemon()
            latch1.countDown()
        }.start()

        assertEquals daemonGroup, actor1.actorGroup
        latch1.await()
        assert daemon

        def actor2 = nonDaemonGroup.actor {
            daemon = Thread.currentThread().isDaemon()
            latch2.countDown()
        }.start()

        assertEquals nonDaemonGroup, actor2.actorGroup
        latch2.await()
        assertFalse daemon

        daemonGroup.shutdown()
        nonDaemonGroup.shutdown()
    }

    public void testGroupsWithActorInheritance() {
        volatile boolean daemon = false;
        final CountDownLatch latch1 = new CountDownLatch(1)
        final CountDownLatch latch2 = new CountDownLatch(1)

        final PooledActorGroup daemonGroup = new PooledActorGroup(false)
        final AbstractActorGroup nonDaemonGroup = new NonDaemonPooledActorGroup()

        final GroupTestActor actor1 = new GroupTestActor(daemonGroup)
        actor1.metaClass.act = {->
            daemon = Thread.currentThread().isDaemon()
            latch1.countDown()
        }
        actor1.start()

        assertEquals daemonGroup, actor1.actorGroup
        latch1.await()
        assert daemon

        final GroupTestActor actor2 = new GroupTestActor(nonDaemonGroup)
        actor2.metaClass.act = {->
            daemon = Thread.currentThread().isDaemon()
            latch2.countDown()
        }
        actor2.start()

        assertEquals nonDaemonGroup, actor2.actorGroup
        latch2.await()
        assertFalse daemon
        daemonGroup.shutdown()
        nonDaemonGroup.shutdown()
    }

    public void testValidGroupReset() {
        final PooledActorGroup daemonGroup = new PooledActorGroup(true)
        final PooledActorGroup nonDaemonGroup = new PooledActorGroup(false)
        final GroupTestActor actor = new GroupTestActor(daemonGroup)

        assertEquals daemonGroup, actor.actorGroup
        actor.actorGroup = nonDaemonGroup
        assertEquals nonDaemonGroup, actor.actorGroup

        daemonGroup.shutdown()
        nonDaemonGroup.shutdown()
    }

    public void testInvalidGroupReset() {
        final PooledActorGroup daemonGroup = new PooledActorGroup(true)
        final PooledActorGroup nonDaemonGroup = new PooledActorGroup(false)
        final GroupTestActor actor = new GroupTestActor(daemonGroup)
        actor.start()
        assertEquals daemonGroup, actor.actorGroup
        shouldFail {
            actor.actorGroup = nonDaemonGroup
        }
        daemonGroup.shutdown()
        nonDaemonGroup.shutdown()
    }

    public void testDifferentPools() {
        final PooledActorGroup daemonGroup1 = new PooledActorGroup(true)
        final PooledActorGroup daemonGroup2 = new PooledActorGroup(true)
        final NonDaemonPooledActorGroup nonDaemonGroup1 = new NonDaemonPooledActorGroup()
        final NonDaemonPooledActorGroup nonDaemonGroup2 = new NonDaemonPooledActorGroup()
        final PooledActorGroup defaultGroup = PooledActors.defaultPooledActorGroup

        assert daemonGroup1.threadPool != daemonGroup2.threadPool
        assert daemonGroup1.threadPool != nonDaemonGroup1.threadPool
        assert daemonGroup1.threadPool != defaultGroup.threadPool

        assert nonDaemonGroup1.threadPool != daemonGroup2.threadPool
        assert nonDaemonGroup1.threadPool != nonDaemonGroup2.threadPool
        assert nonDaemonGroup1.threadPool != defaultGroup.threadPool
    }
}

class GroupTestActor extends AbstractPooledActor {

    def GroupTestActor(AbstractActorGroup group) {
        actorGroup = group
    }

    protected void act() {
    }
}
