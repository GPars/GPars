package groovyx.gpars.benchmark.embarrassinglyParallel

import groovyx.gpars.GParsExecutorsPool
import groovyx.gpars.GParsPool

final numbers = (0L..100000L).collect() {it}
def sum = 1
2.times {
    sum = measureFJPool(numbers, sum)
}

final t1 = System.currentTimeMillis()
2.times {
    sum = measureFJPool(numbers, sum)
}
final t2 = System.currentTimeMillis()

2.times {
    sum = measurePool(numbers, sum)
}

final t3 = System.currentTimeMillis()
2.times {
    sum = measurePool(numbers, sum)
}
final t4 = System.currentTimeMillis()

println sum
println "FJ pool: " + (t2 - t1)
println "Executors pool: " + (t4 - t3)

private long measureFJPool(numbers, sum) {
    GParsPool.withPool {
        (numbers.everyParallel {it >= 0}) ? sum : 0
    }
}

private long measurePool(numbers, sum) {
    GParsExecutorsPool.withPool {
        (numbers.everyParallel {it >= 0}) ? sum : 0
    }
}