import org.gparallelizer.actors.pooledActors.DefaultPool
import org.gparallelizer.actors.pooledActors.FJPool
import java.util.concurrent.CyclicBarrier
import org.gparallelizer.actors.pooledActors.Pool
import org.gparallelizer.actors.pooledActors.ResizablePool
import org.gparallelizer.actors.pooledActors.ResizableFJPool

List items = []
for (i in 1..100000) {items << {i+it}}

final def numOfIterations = 1..100
final def numOfWarmupIterations = 1..100


meassureSequential(numOfWarmupIterations, items)
final long time = meassureSequential(numOfIterations, items)
println "Sequential $time"

meassurePool(numOfWarmupIterations, items, new DefaultPool(true, 3))
time = meassurePool(numOfIterations, items, new DefaultPool(true, 3))
println "Default Pool $time"

meassurePool(numOfWarmupIterations, items, new ResizablePool(true, 3))
time = meassurePool(numOfIterations, items, new ResizablePool(true, 3))
println "Resizable Pool $time"

meassurePool(numOfWarmupIterations, items, new FJPool(3))
time = meassurePool(numOfIterations, items, new FJPool(3))
println "FJ Pool $time"

meassurePool(numOfWarmupIterations, items, new ResizableFJPool(3))
time = meassurePool(numOfIterations, items, new ResizableFJPool(3))
println "Resizable FJ Pool $time"

long meassureSequential(iterations, List tasks) {
    final long t1 = System.currentTimeMillis()
    for (i in iterations) {
        for(task in tasks) {
            int result = task.call(10)
            if (result < 0) println result
        }
    }
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}

long meassurePool(iterations, List tasks, Pool pool) {
    final long t1 = System.currentTimeMillis()
    for (i in iterations) {
        for(task in tasks) {
            pool.execute {
                int result = task.call(10)
                if (result < 0) println result
            }
        }
        pause(pool)
    }
    pool.shutdown()
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}

private def pause(Pool pool) {
    final CyclicBarrier barrier = new CyclicBarrier(2)
    pool.execute { barrier.await() }
    barrier.await()
}
