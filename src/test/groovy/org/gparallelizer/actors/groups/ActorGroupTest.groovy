package org.gparallelizer.actors.groups

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actors
import org.gparallelizer.actors.ActorGroup
import org.gparallelizer.actors.AbstractActor
import org.gparallelizer.actors.ActorMessage
import java.util.concurrent.LinkedBlockingQueue

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
        assertFalse daemon
    }

    public void testGroupDaemonFlag() {
        volatile boolean daemon = false;
        final CountDownLatch latch1 = new CountDownLatch(1)
        final CountDownLatch latch2 = new CountDownLatch(1)

        final ActorGroup daemonGroup = new ActorGroup(true)
        final ActorGroup nonDaemonGroup = new ActorGroup(false)

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

        final ActorGroup daemonGroup = new ActorGroup(true)
        final ActorGroup nonDaemonGroup = new ActorGroup(false)

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
        final ActorGroup daemonGroup = new ActorGroup(true)
        final ActorGroup nonDaemonGroup = new ActorGroup(false)
        final GroupTestActor actor = new GroupTestActor(daemonGroup)

        assertEquals daemonGroup, actor.actorGroup
        actor.actorGroup = nonDaemonGroup
        assertEquals nonDaemonGroup, actor.actorGroup

    }

    public void testInvalidGroupReset() {
        final ActorGroup daemonGroup = new ActorGroup(true)
        final ActorGroup nonDaemonGroup = new ActorGroup(false)
        final GroupTestActor actor = new GroupTestActor(daemonGroup)
        actor.start()
        assertEquals daemonGroup, actor.actorGroup
        shouldFail {
            actor.actorGroup = nonDaemonGroup
        }
    }

    public void testDifferentThreadFactories() {
        final ActorGroup daemonGroup1 = new ActorGroup(true)
        final ActorGroup daemonGroup2 = new ActorGroup(true)
        final ActorGroup nonDaemonGroup1 = new ActorGroup(false)
        final ActorGroup nonDaemonGroup2 = new ActorGroup(false)
        final ActorGroup defaultGroup = Actors.defaultActorGroup

        assert daemonGroup1.threadFactory != daemonGroup2.threadFactory
        assert daemonGroup1.threadFactory != nonDaemonGroup1.threadFactory
        assert daemonGroup1.threadFactory != defaultGroup.threadFactory

        assert nonDaemonGroup1.threadFactory != daemonGroup2.threadFactory
        assert nonDaemonGroup1.threadFactory != nonDaemonGroup2.threadFactory
        assert nonDaemonGroup1.threadFactory != defaultGroup.threadFactory
    }
}

class GroupTestActor extends AbstractActor {

    def GroupTestActor(ActorGroup group) {
        super(new LinkedBlockingQueue<ActorMessage>());
        actorGroup = group
    }

    protected void act() {
    }
}