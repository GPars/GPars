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

package groovyx.gpars;

import groovyx.gpars.dataflow.DataFlowVariable;
import jsr166y.forkjoin.RecursiveAction;
import jsr166y.forkjoin.TaskBarrier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the contract between the ForkJoinOrchestrator and the task workers.
 * Subclasses need to implement the compute() to perform the actual Fork/Join algorithm leveraging the options
 * provided by the AbstractForkJoinWorker class.
 *
 * Author: Vaclav Pech
 * Date: Nov 1, 2009
 */
@SuppressWarnings({"AbstractClassWithOnlyOneDirectInheritor", "CollectionWithoutInitialCapacity"})
public abstract class AbstractForkJoinWorker<T> extends RecursiveAction {

    /**
     * Stores the result of the worker
     */
    private final DataFlowVariable<T> value = new DataFlowVariable<T>();

    /**
     * The barrier obtained from the parent worker
     */
    @SuppressWarnings({"InstanceVariableMayNotBeInitialized", "FieldHasSetterButNoGetter"})
    private TaskBarrier taskBarrier;

    /**
     * A barrier to synchronize with the child workers on
     */
    private final TaskBarrier childTaskBarrier = new TaskBarrier(1);

    /**
     * Stores the child workers
     */
    private final List<AbstractForkJoinWorker<T>> children = new ArrayList<AbstractForkJoinWorker<T>>();

    protected AbstractForkJoinWorker() { }

    /**
     * Sets the parent barrier which it waits on for all workers to finish their work.
     * The worker is supposed to register to the barrier and call arriveAndDeregister() once it has completed its work.
     * @param taskBarrier The barrier to use for communication with the parent
     */
    final void setTaskBarrier(final TaskBarrier taskBarrier) {
        if (taskBarrier==null) return;
        this.taskBarrier = taskBarrier;
        this.taskBarrier.register();
    }

    /**
     * Retrieves the result of the worker
     * @return The value calculated by the worker
     * @throws java.lang.InterruptedException If interrupted while waiting for the result
     */
    public final T getResult() throws InterruptedException {
        return value.getVal();
    }

    /**
     * Sets the result of the calculation of the worker. Informs the parent about completion.
     * @param value The result to store and report to the parent worker
     */
    protected final void setResult(final T value) {
        this.value.bind(value);
        taskBarrier.arriveAndDeregister();
    }

    /**
     * Blocks until all children finish their calculations, releasing the physical thread temporarily to the thread pool
     * to do some work stealing. It is not ususally necessary to call the awaitChildren() method directly
     * since the getChildrenResults() method calls it itself before returning the children results. 
     */
    protected final void awaitChildren() {
        childTaskBarrier.arriveAndAwait();
    }

    /**
     * Forks a child task. Makes sure it has a means to indicate back completion.
     * The worker is stored in the internal list of workers for evidence and easy result retrieval through getChildrenResults().
     * @param child The child task
     */
    protected final void forkOffChild(final AbstractForkJoinWorker<T> child) {
        children.add(child);
        assert taskBarrier != null;
        child.setTaskBarrier(childTaskBarrier);
        child.fork();
    }

    /**
     * Retrieves the unmodifiable list of workers.
     * @return All workers forked through the forkOff() method.
     */
    protected final List<AbstractForkJoinWorker<T>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Waits for and returns the results of the child tasks.
     * @return A list of results returned from the child tasks
     * @throws InterruptedException If the current thread got interrupted while waiting for the results
     */
    protected final List<T> getChildrenResults() throws InterruptedException {
        awaitChildren();
        final List<T> results = new ArrayList<T>();
        for (final AbstractForkJoinWorker<T> worker : getChildren()) {
            results.add(worker.getResult());
        }
        return results;
    }
}
