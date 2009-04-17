import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import static org.gparallelizer.actors.pooledActors.PooledActors.actor
import static org.gparallelizer.actors.pooledActors.PooledActors.getPool

class MyActor extends AbstractPooledActor {

    protected void act() {
        handleA()
    }

    private void handleA() {
        react {a ->
            replyIfExists "Done"
            handleB(a)
        }
    }

    private void handleB(int a) {
        react {b ->
            println a + b
            LifeCycleHelper.latch.countDown()
        }
    }
}
testActor(new MyActor())


Actor actor2 = actor {
    handleA()
}

actor2.metaClass {
    handleA = {->
        react {a ->
            replyIfExists "Done"
            handleB(a)
        }
    }

    handleB = {a ->
        react {b ->
            println a + b
            LifeCycleHelper.latch.countDown()
        }
    }
}
testActor(actor2)


Closure handleB = {a ->
    react {b ->
        println a + b
        LifeCycleHelper.latch.countDown()
    }
}

Closure handleA = {->
    react {a ->
        replyIfExists "Done"
        handleB(a)
    }
}

Actor actor3 = actor {
    handleA()
}
handleA.delegate = actor3
handleB.delegate = actor3
testActor(actor3)


LifeCycleHelper.latch.await()
getPool().shutdown()


class LifeCycleHelper {
    static CountDownLatch latch = new CountDownLatch(3)
}

private def testActor(AbstractPooledActor actor) {
    actor.start()
    actor.send 2
    actor.send 3
}
