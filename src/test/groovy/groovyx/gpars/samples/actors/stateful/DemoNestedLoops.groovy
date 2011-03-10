// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

import groovyx.gpars.actor.DefaultActor

/**
 * Demonstrates a way to do continuation-style loops with Actors.
 * @author Vaclav Pech
 */
class MyLoopActor extends DefaultActor {

    protected void act() {
        loop {
            outerLoop()
        }
    }

    private void outerLoop() {
        react {a ->
            println 'Outer: ' + a
            if (a != 0) innerLoop()
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

actor.with {
    start()

    send 1
    send 1
    send 1
    send 1
    send 1
    send 0
    send 2
    send 2
    send 2
    send 2
    send 2
    send 0
    send 3
    send 3
    send 3
    send 3
    send 0
    send 0

    Thread.sleep 2000
    send 4
    Thread.sleep 1000
    stop()
}

