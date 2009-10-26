package groovyx.gpars.benchmark

import groovyx.gpars.actor.PooledActorGroup

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
        final PooledActorGroup group = new PooledActorGroup(1)
        def a1 = group.actor {
            perform(iterations)
        }
        a1.start()
        a1.join()

    }

    static void actorPerform() {
        final PooledActorGroup group = new PooledActorGroup(2)
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
        [a1, a2]*.start()
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

