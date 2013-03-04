// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

class Foo {
    public static final Integer iterations = 10000000

    static BigDecimal perform(Integer num) {
        BigDecimal sum = 0
        for (int i in 1..num.intValue()) {
            sum += 6.3 * i + i * 2 + new Random().nextInt(i)
        }
        return sum
    }

    static void singleThreadPerform() {
        def thread1 = Thread.start {
            perform(10000000)
        }
        thread1.join()
    }

    static void threadsPerform() {
        def thread1 = Thread.start {
            perform(10000000.intdiv(2).toInteger())
        }
        def thread2 = Thread.start {
            perform(10000000.intdiv(2).toInteger())
        }
        thread1.join()
        thread2.join()
    }

    static void threadsPerform(int num) {
        List<Thread> threads = (1..num).collect {
            Thread.start {
                perform(10000000.intdiv(num).toInteger())
            }
        }

        threads.each { Thread t -> t.join() }
    }

//    static void singleActorPerform() {
//        final DefaultPGroup group = new DefaultPGroup(1)
//        def a1 = group.actor {
//            perform(iterations)
//        }
//        a1.join()
//
//    }

//    static void actorPerform() {
//        final DefaultPGroup group = new DefaultPGroup(2)
//        def a1 = group.actor {
//            perform(iterations.intdiv(2).toInteger())
//        }
//        def a2 = group.actor {
//            perform(iterations.intdiv(2).toInteger())
//        }
//        a1.start()
//        a1.join()
//        a2.start()
//        a2.join()
//        [a1, a2]*.join()
//
//    }
}

//Foo.perform(Foo.iterations)
//long t1 = System.currentTimeMillis()
//Foo.perform(Foo.iterations)
//println 'Direct invocation ' + (System.currentTimeMillis() - t1)

Foo.singleThreadPerform()
t1 = System.currentTimeMillis()
Foo.singleThreadPerform()
println 'One thread invocation ' + (System.currentTimeMillis() - t1)

Foo.threadsPerform()
t1 = System.currentTimeMillis()
Foo.threadsPerform()
println 'Two threads invocation ' + (System.currentTimeMillis() - t1)

Foo.threadsPerform(4)
t1 = System.currentTimeMillis()
Foo.threadsPerform(4)
println 'Four threads invocation ' + (System.currentTimeMillis() - t1)

Foo.threadsPerform(8)
t1 = System.currentTimeMillis()
Foo.threadsPerform(8)
println 'Eight threads invocation ' + (System.currentTimeMillis() - t1)

//Foo.singleActorPerform()
//t1 = System.currentTimeMillis()
//Foo.singleActorPerform()
//println 'One actor invocation ' + (System.currentTimeMillis() - t1)
//
//Foo.actorPerform()
//t1 = System.currentTimeMillis()
//Foo.actorPerform()
//println 'Two actor invocation ' + (System.currentTimeMillis() - t1)

