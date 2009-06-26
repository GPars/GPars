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
