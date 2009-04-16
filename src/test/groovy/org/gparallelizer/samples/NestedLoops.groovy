import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.PooledActors
import static org.gparallelizer.actors.pooledActors.PooledActors.actor
import static org.gparallelizer.actors.pooledActors.PooledActors.getPool
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

class MyLoopActor extends AbstractPooledActor {

    protected void act() {
        outerLoop()
    }

    private void outerLoop() {
        react {a ->
            println 'Outer: ' + a
            innerLoop()
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

Thread.sleep 5000
getPool().shutdown()

