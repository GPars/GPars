/**
 * Demonstrates asynchronous collection processing using ParallelArrays through the Parallelizer class.
 * Requires the jsr166y jar on the class path.
 */

import org.gparallelizer.Parallelizer

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

Parallelizer.withParallelizer {
    println list.collectAsync {it * 2 }

    list.iterator().eachAsync {
        println it
    }
}

