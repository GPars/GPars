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

    final String text = 'want to be big'
    println((text.collectAsync {it.toUpperCase()}).join())

    def animals = ['dog', 'ant', 'cat', 'whale']
    println (animals.anyAsync {it ==~ /ant/} ? 'Found an ant' : 'No ants found')
    println (animals.allAsync {it.contains('a')} ? 'All animals contain a' : 'Some animals can live without an a')
}

