//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package groovyx.gpars

import groovyx.gpars.dataflow.DataFlowVariable
import jsr166y.forkjoin.RecursiveAction
import jsr166y.forkjoin.TaskBarrier

/**
 * Orchestrates a Fork/Join algorithm hiding all the details of manipulating TaskBarriers and starting the sub-tasks.
 * A root worker implementation must be provided as an instance of a custom AbstractForkJoinWorker subclass.
 * The worker will calculate the root of the problem, most likely starting other workers
 * to solve their respective sup-problems.
 * The ForkJoinOrchestrator relies on being invoked inside the Parallelizer.doParallel() block.
 * The Parallelizer.orchestrate() method can be used as a useful shorthand for instantiating a ForkJoinOrchestrator
 * and calling start() plus getResult() on it.
 * Parallelizer.orchestrate(rootWorker) is equivalent to
 * new ForkJoinOrchestrator(rootWorker).start().getResult()
 * Calls to getResult() block the caller and to allow the caller to do some work concurrently with the Fork/Join tasks,
 * it is not necessary to call it immediately after starting the calculation with start().
 *
 * Author: Vaclav Pech
 * Date: Nov 1, 2009
 */
public final class ForkJoinOrchestrator<T> extends RecursiveAction {
    private final DataFlowVariable<T> result = new DataFlowVariable<T>()
    private final AbstractForkJoinWorker<T> rootWorker

    /**
     * Creates a new instance with the given root worker.
     * @param rootWorker The root worker, which will be eventually invoked to start the whole algorithm
     */
    public def ForkJoinOrchestrator(final rootWorker) {
        this.rootWorker = rootWorker;
    }

    /**
     * Starts the algorithm.
     * The ForkJoinOrchestrator relies on being invoked inside the Parallelizer.doParallel() block.
     */
    public ForkJoinOrchestrator<T> start() {
        def pool = Parallelizer.retrieveCurrentPool()
        if (pool==null) throw new IllegalStateException("Cannot start an ForkJoinOrchestrator. The pool has not been set. Perhaps, we're no inside a Parallelizer.doParallel() block.")
        pool.execute(this)
        return  this
    }

    /**
     * Starts the algorithm and waits for the result.
     * Blocks until a result is available.
     * The ForkJoinOrchestrator relies on being invoked inside the Parallelizer.doParallel() block.
     * @return The result returned by the root worker.
     */
    public T perform() {
        start().getResult()
    }

    /**
     * Starts the root worker, waits for the calculation to finish and retrieves the result from the root worker.
     */
    void compute() {
        final TaskBarrier taskBarrier = new TaskBarrier(1)
        rootWorker.taskBarrier = taskBarrier
        rootWorker.fork()
        taskBarrier.arriveAndAwait()
        result << rootWorker.result
    }

    /**
     * Retrieves the result of the calculation.
     * Blocks until a result is available.
     * @return The result returned by the root worker.
     */
    public T getResult() {
        result.val
    }
}
