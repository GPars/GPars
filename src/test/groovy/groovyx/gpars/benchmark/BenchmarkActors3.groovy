package groovyx.gpars.benchmark

/*
*  Calculation of Pi using quadrature realized with GPars actors.
*
*  Copyright © 2009 Russel Winder.  All rights reserved.
*/

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.LinkedBlockingQueue

def execute(actorCount) {
    final long n = 100000000l // 10 times fewer due to speed issues.
    final double delta = 1.0d / n
    final long sliceSize = n / actorCount
    final long startTimeNanos = System.nanoTime()
    final computors = []
//    final DataFlowStream stream = new DataFlowStream()
    final ArrayBlockingQueue queue = new ArrayBlockingQueue(actorCount)
    final accumulator = new Thread ({
        double sum = 0.0d
        for (c in computors) {sum += queue.take()}
        final double pi = 4.0d * sum * delta
        final double elapseTime = (System.nanoTime() - startTimeNanos) / 1e9
        println("==== Groovy GPars ActorScript pi = " + pi)
        println("==== Groovy GPars ActorScript iteration count = " + n)
        println("==== Groovy GPars ActorScript elapse = " + elapseTime)
        println("==== Groovy GPars ActorScript actor count = " + actorCount)
    })

    (0l..<actorCount).each {long index ->
        final long start = 1l + index * sliceSize
        final long end = (index + 1l) * sliceSize
        computors.add(
                Thread.start {
                    double sum = 0.0d
                    for (long i = start; i <= end; ++i) {
                        final double x = (i - 0.5d) * delta
                        sum += 1.0d / (1.0d + x * x)
                    }
                    queue.offer(sum)
                }
        )
    }
    accumulator.start()
//    for (c in computors) { c.start() }
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

