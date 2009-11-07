package groovyx.gpars

import groovyx.gpars.scheduler.FJPool

// only a spike - just to get the discussion rolling
// Author: Dierk Koenig

final class ParallelS {
    @Delegate adaptee
    private FJPool threadPool
    private final static FJPool sharedThreadPool = new FJPool()

    def each(Closure yield) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.eachParallel(yield) } }
    def eachWithIndex(Closure yield, index) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.eachWithIndexParallel(yield, index) } }
    def collect(Closure yield) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.collectParallel(yield) } }
    def find(Closure yield) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.findParallel(yield) } }
    def findAll(Closure yield) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.findAllParallel(yield) } }
    def grep(filter) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.grepParallel(filter) } }
    def all(Closure yield) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.everyParallel(yield) } }
    def any(Closure yield) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.anyParallel(yield) } }
    def groupBy(Closure yield) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.groupByParallel(yield) } }

    public void shutdown() {
        if (threadPool!=sharedThreadPool) {
            threadPool.shutdown()
        }
    }

    static void prepare(obj) {
        obj.metaClass.getParallel = {->
            new ParallelS(adaptee: delegate, threadPool:sharedThreadPool)
        }
    }

    static void prepare(obj, FJPool threadPool) {
        obj.metaClass.getParallel = {->
            new ParallelS(adaptee: delegate, threadPool:threadPool)
        }
    }
}

// works on objects
def obj = (1..6).toList()
ParallelS.prepare obj

def threadPrinter = { println Thread.currentThread() }

obj.each threadPrinter
obj.parallel.each threadPrinter
obj.each threadPrinter

// but just as well on classes or interfaces
ParallelS.prepare Set
def set = (1..6) as Set
set.parallel.each threadPrinter
set.each threadPrinter

final List items = [1, 2, 3, 4, 5]
ParallelS.prepare items
Collection c = items.parallel  //will fail


def nums = (1..20)
ParallelS.prepare nums, new FJPool()
final def pnums = nums.parallel
assert 10 == pnums.grep(1..10).size()
assert 5 == pnums.grep(1..5).size()
nums.parallel.shutdown()
//should fail
//pnums.each {}