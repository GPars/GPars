package org.gparallelizer.samples

import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.PooledActors
import static org.gparallelizer.actors.pooledActors.PooledActors.actor

/**
 * Demonstrates a way to do continuation-style loops with PooledActors.
 * @author Vaclav Pech
 */

Closure innerLoop

Closure outerLoop = {->
    react {a ->
        println 'Outer: ' + a
        innerLoop()
    }
}

innerLoop = {->
    react {b ->
        println 'Inner ' + b
        if (b==0) outerLoop()
        else innerLoop()
    }
}

Actor actor = actor {
    outerLoop()
}
outerLoop.delegate = actor
innerLoop.delegate = actor

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

Thread.sleep 5000
PooledActors.defaultPooledActorGroup.shutdown()

