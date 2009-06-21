import org.gparallelizer.Asynchronizer

/**
 * Demonstrates asynchronous collection processing using Executor services through the Asynchronizer class.
 */

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

Asynchronizer.withAsynchronizer {
    println list.collectAsync {it * 2 }

    list.iterator().eachAsync {
        println it
    }
    
    if (list.allAsync{it < 10 }) println 'The list contains only small numbers.'

    final String text = 'want to be big'
    println((text.collectAsync {it.toUpperCase()}).join())

    def animals = ['dog', 'ant', 'cat', 'whale']
    println (animals.anyAsync {it ==~ /ant/} ? 'Found an ant' : 'No ants found')
    println (animals.allAsync {it.contains('a')} ? 'All animals contain a' : 'Some animals can live without an a')
}

