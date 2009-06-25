package org.gparallelizer.actors

public class JoinTest extends GroovyTestCase {
    public void testActorJoin() {
        final def actor = Actors.actor { Thread.sleep 500; stop() }.start()
        actor.join()
        assertFalse actor.isActive()
    }

    public void testOneShotActorJoin() {
        final def actor = Actors.oneShotActor { Thread.sleep 500 }.start()
        actor.join()
        assertFalse actor.isActive()
    }

    public void testCooperatingActorJoin() {
        final def actor1 = Actors.oneShotActor { receive() }.start()
        final def actor2 = Actors.oneShotActor {actor1.join()}.start()
        actor1 << 'Message'
        [actor1, actor2]*.join()
        assertFalse actor1.isActive()
        assertFalse actor2.isActive()
    }

    public void testStoppedActorJoin() {
        final def actor = Actors.oneShotActor { }.start()
        actor.join()
        assertFalse actor.isActive()
        actor.join()
        assertFalse actor.isActive()
    }
}