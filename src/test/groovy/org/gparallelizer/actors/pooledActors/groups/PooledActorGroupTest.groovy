package org.gparallelizer.actors.pooledActors.groups

import org.gparallelizer.actors.pooledActors.PooledActors
import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: May 6, 2009
 * Time: 2:49:04 PM
 * To change this template use File | Settings | File Templates.
 */

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

        final PooledActorGroup daemonGroup = new PooledActorGroup(true)
        final PooledActorGroup nonDaemonGroup = new PooledActorGroup(false)

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

        daemonGroup.threadPool.shutdown()
        nonDaemonGroup.threadPool.shutdown()
    }

    public void testGroupsWithActorInheritance() {
        volatile boolean daemon = false;
        final CountDownLatch latch1 = new CountDownLatch(1)
        final CountDownLatch latch2 = new CountDownLatch(1)

        final PooledActorGroup daemonGroup = new PooledActorGroup(true)
        final PooledActorGroup nonDaemonGroup = new PooledActorGroup(false)

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
        daemonGroup.threadPool.shutdown()
        nonDaemonGroup.threadPool.shutdown()
    }

    public void testValidGroupReset() {
        final PooledActorGroup daemonGroup = new PooledActorGroup(true)
        final PooledActorGroup nonDaemonGroup = new PooledActorGroup(false)
        final GroupTestActor actor = new GroupTestActor(daemonGroup)

        assertEquals daemonGroup, actor.actorGroup
        actor.actorGroup = nonDaemonGroup
        assertEquals nonDaemonGroup, actor.actorGroup

        daemonGroup.threadPool.shutdown()
        nonDaemonGroup.threadPool.shutdown()
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
        daemonGroup.threadPool.shutdown()
        nonDaemonGroup.threadPool.shutdown()
    }

    public void testDifferentPools() {
        final PooledActorGroup daemonGroup1 = new PooledActorGroup(true)
        final PooledActorGroup daemonGroup2 = new PooledActorGroup(true)
        final PooledActorGroup nonDaemonGroup1 = new PooledActorGroup(false)
        final PooledActorGroup nonDaemonGroup2 = new PooledActorGroup(false)
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

    def GroupTestActor(PooledActorGroup group) {
        actorGroup = group
    }

    protected void act() {
    }
}