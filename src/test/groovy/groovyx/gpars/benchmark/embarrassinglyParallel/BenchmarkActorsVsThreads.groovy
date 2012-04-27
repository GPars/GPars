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

package groovyx.gpars.benchmark.embarrassinglyParallel

import groovyx.gpars.group.DefaultPGroup

class Foo {
    static final def iterations = 4000000

    static def perform(num) {
        def sum = 0
        for (i in 1..num) {
//            sum += 6.3 * i + i * 2
            new Random().nextInt(i)
        }
        return sum
    }

    static void singleThreadPerform() {
        def thread1 = Thread.start {
            perform(iterations)
        }
        thread1.join()
    }

    static void threadsPerform() {
        def thread1 = Thread.start {
            perform(iterations / 2)
        }
        def thread2 = Thread.start {
            perform(iterations / 2)
        }
        [thread1, thread2]*.join()
    }

    static void singleActorPerform() {
        final DefaultPGroup group = new DefaultPGroup(1)
        def a1 = group.actor {
            perform(iterations)
        }
        a1.join()

    }

    static void actorPerform() {
        final DefaultPGroup group = new DefaultPGroup(2)
        def a1 = group.actor {
            perform(iterations / 2)
        }
        def a2 = group.actor {
            perform(iterations / 2)
        }
//        a1.start()
//        a1.join()
//        a2.start()
//        a2.join()
        [a1, a2]*.join()

    }
}

Foo.perform(Foo.iterations)
long t1 = System.currentTimeMillis()
Foo.perform(Foo.iterations)
println 'Direct invocation ' + (System.currentTimeMillis() - t1)

Foo.singleThreadPerform()
t1 = System.currentTimeMillis()
Foo.singleThreadPerform()
println 'One thread invocation ' + (System.currentTimeMillis() - t1)

Foo.threadsPerform()
t1 = System.currentTimeMillis()
Foo.threadsPerform()
println 'Two threads invocation ' + (System.currentTimeMillis() - t1)

Foo.singleActorPerform()
t1 = System.currentTimeMillis()
Foo.singleActorPerform()
println 'One actor invocation ' + (System.currentTimeMillis() - t1)

Foo.actorPerform()
t1 = System.currentTimeMillis()
Foo.actorPerform()
println 'Two actor invocation ' + (System.currentTimeMillis() - t1)

