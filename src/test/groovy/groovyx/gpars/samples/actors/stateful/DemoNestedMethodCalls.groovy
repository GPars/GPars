// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.actors.stateful

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import java.util.concurrent.CountDownLatch
import static groovyx.gpars.actor.Actors.actor

/**
 * Demonstrates various ways to structure pooled actor's code with its continuation-style message handling.
 * @author Vaclav Pech
 */

class MyPooledActor extends DefaultActor {

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
testActor(new MyPooledActor().start())


Actor actor2 = actor {
    delegate.metaClass {
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

    handleA()
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
    handleA.delegate = delegate
    handleB.delegate = delegate

    handleA()
}
testActor(actor3)


LifeCycleHelper.latch.await()


class LifeCycleHelper {
    static CountDownLatch latch = new CountDownLatch(3)
}

private def testActor(Actor actor) {
    actor.send 2
    actor.send 3
}
