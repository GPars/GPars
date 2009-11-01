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

/**
 * Implements the contract between the ForkJoinOrchestrator and the workers as defined by ForkJoinWorker.
 *
 * Author: Vaclav Pech
 * Date: Nov 1, 2009
 */
@SuppressWarnings({"AbstractClassWithOnlyOneDirectInheritor"})
public abstract class AbstractForkJoinWorker<T> extends RecursiveAction implements ForkJoinWorker<T> {
    private final DataFlowVariable<T> value = new DataFlowVariable<T>();
    private TaskBarrier taskBarrier;
    private final TaskBarrier childTaskBarrier = new TaskBarrier(1);

    protected AbstractForkJoinWorker() { }

    /**
     * Sets the parent barrier which it waits on for all workers to finish their work.
     * The worker is supposed to register to the barrier and call arriveAndDeregister() once it has completed its work.
     * @param taskBarrier The barrier to use for communication with the parent
     */
    public final void setTaskBarrier(final TaskBarrier taskBarrier) {
        if (taskBarrier==null) return;
        this.taskBarrier = taskBarrier;
        this.taskBarrier.register();
    }

    /**
     * Retrieves the result of the worker
     * @return The value calculated by the worker
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
     * to do some work stealing.
     */
    protected final void awaitChildren() {
        childTaskBarrier.arriveAndAwait();
    }

    /**
     * Forks a child task. Makes sure it has a means to indicate back completion.
     * @param child The child task
     */
    protected final void forkOffChild(final AbstractForkJoinWorker child) {
        assert taskBarrier!=null;
        child.setTaskBarrier(childTaskBarrier);
        child.fork();
    }
}
