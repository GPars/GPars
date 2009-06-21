import org.gparallelizer.AsyncEnhancer

/**
 * Demonstrates asynchronous collection processing using ParallelArrays through the ParallelEnhancer class.
 * Requires the jsr166y jar on the class path.
 */

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

AsyncEnhancer.enhanceInstance(list)

println list.collectAsync {it * 2 }

final Iterator iterator = list.iterator()
AsyncEnhancer.enhanceInstance iterator

iterator.eachAsync {
    println it
}

final String text = 'want to be big'
AsyncEnhancer.enhanceInstance text
println((text.collectAsync {it.toUpperCase()}).join())

def animals = ['dog', 'ant', 'cat', 'whale']
AsyncEnhancer.enhanceInstance animals
println (animals.anyAsync {it ==~ /ant/} ? 'Found an ant' : 'No ants found')
println (animals.allAsync {it.contains('a')} ? 'All animals contain a' : 'Some animals can live without an a')
