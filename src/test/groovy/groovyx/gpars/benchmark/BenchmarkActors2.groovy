package groovyx.gpars.benchmark

/*
*  Calculation of Pi using quadrature realized with GPars actors.
*
*  Copyright © 2009 Russel Winder.  All rights reserved.
*/

import groovyx.gpars.actor.PooledActorGroup

def execute(int actorCount) {
    final long n = 100000000l // 10 times fewer due to speed issues.
    final double delta = 1.0d / n
    final long sliceSize = n / actorCount
    final long startTimeNanos = System.nanoTime()
    final List computors = []
    final group = new PooledActorGroup(actorCount) // Interesting (!) behaviour with the +1 missing.
    final accumulator = group.actor {
        double sum = 0.0d
        int counter = 0
        loop {
            if (counter < computors.size()) {
                counter++
                react { sum += it }
            } else {
                final double pi = 4.0d * sum * delta
                final double elapseTime = (System.nanoTime() - startTimeNanos) / 1e9
                println("==== Groovy GPars ActorScript pi = " + pi)
                println("==== Groovy GPars ActorScript iteration count = " + n)
                println("==== Groovy GPars ActorScript elapse = " + elapseTime)
                println("==== Groovy GPars ActorScript actor count = " + actorCount)
                stop()
            }
        }
    }
    (0..<actorCount).each {long index ->
        final long start = 1l + index * sliceSize
        final long end = (index + 1l) * sliceSize
        computors.add(
                group.actor {
                    double sum = 0.0d
                    for (long i = start; i <= end; ++i) {
                        final double x = (i - 0.5d) * delta
                        sum += 1.0d / (1.0d + x * x)
                    }
                    accumulator << sum
                }
        )

    }
    accumulator.join()
}

execute(1)
println()
execute(2)
println()
execute(3)
println()
execute(8)
println()
execute(32)

