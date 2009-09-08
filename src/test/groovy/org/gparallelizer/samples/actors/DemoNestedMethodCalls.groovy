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

package org.gparallelizer.samples.actors

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import static org.gparallelizer.actors.pooledActors.PooledActors.actor
import org.gparallelizer.actors.pooledActors.PooledActors

/**
 * Demonstrates various ways to structure pooled actor's code with its continuation-style message handling.
 * @author Vaclav Pech
 */

class MyPooledActor extends AbstractPooledActor {

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
testActor(new MyPooledActor())


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
PooledActors.defaultPooledActorGroup.shutdown()


class LifeCycleHelper {
    static CountDownLatch latch = new CountDownLatch(3)
}

private def testActor(AbstractPooledActor actor) {
    actor.start()
    actor.send 2
    actor.send 3
}
