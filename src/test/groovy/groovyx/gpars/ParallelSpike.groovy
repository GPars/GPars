package groovyx.gpars

import groovyx.gpars.scheduler.FJPool

// only a spike - just to get the discussion rolling
// Author: Dierk Koenig

class Parallel {
  @Delegate adaptee
  private final static FJPool threadPool = new FJPool()

  def each(Closure yield) { Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) { adaptee.eachAsync(yield) } }

  static void prepare(obj) {
    obj.metaClass.getParallel = {->
      new Parallel(adaptee: delegate)
    }
  }
}

// works on objects
def obj = (1..6).toList()
Parallel.prepare obj

def threadPrinter = { println Thread.currentThread() }

obj.each threadPrinter
obj.parallel.each threadPrinter
obj.each threadPrinter

// but just as well on classes or interfaces
Parallel.prepare Set
def set = (1..6) as Set
set.parallel.each threadPrinter
set.each threadPrinter