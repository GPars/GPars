package org.gparallelizer.samples

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActors

/**
 * Demonstrates a way to do continuation-style loops with PooledActors.
 * @author Vaclav Pech
 */
class MyLoopActor extends AbstractPooledActor {

    protected void act() {
        loop {
            outerLoop()
        }
    }

    private void outerLoop() {
        react {a ->
            println 'Outer: ' + a
            if (a!=0) innerLoop()
            else println 'Done'
        }
    }

    private void innerLoop() {
        react {b ->
            println 'Inner ' + b
            if (b == 0) outerLoop()
            else innerLoop()
        }
    }
}

MyLoopActor actor = new MyLoopActor()

actor.start()

actor.send 1
actor.send 1
actor.send 1
actor.send 1
actor.send 1
actor.send 0
actor.send 2
actor.send 2
actor.send 2
actor.send 2
actor.send 2
actor.send 0
actor.send 3
actor.send 3
actor.send 3
actor.send 3
actor.send 0
actor.send 0



Thread.sleep 3000
actor.send 4
Thread.sleep 3000

PooledActors.defaultPooledActorGroup.shutdown()

