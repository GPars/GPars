package org.gparallelizer.actors.pooledActors

public class JoinTest extends GroovyTestCase {
    public void testActorJoin() {
        final def actor = PooledActors.actor { Thread.sleep 500; stop() }.start()
        actor.join()
        assertFalse actor.isActive()
    }

    public void testOneShotActorJoin() {
        final def actor = PooledActors.actor { Thread.sleep 500 }.start()
        actor.join()
        assertFalse actor.isActive()
    }

    public void testCooperatingActorJoin() {
        final def actor1 = PooledActors.actor { react{} }.start()
        final def actor2 = PooledActors.actor {actor1.join()}.start()
        actor1 << 'Message'
        [actor1, actor2]*.join()
        assertFalse actor1.isActive()
        assertFalse actor2.isActive()
    }

    public void testStoppedActorJoin() {
        final def actor = PooledActors.actor { }.start()
        actor.join()
        assertFalse actor.isActive()
        actor.join()
        assertFalse actor.isActive()
    }
}