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

import jsr166y.forkjoin.RecursiveAction;
import jsr166y.forkjoin.TaskBarrier;

/**
 * Implements the contract between the ForkJoinOrchestrator and the workers as defined by ForkJoinWorker.
 *
 * Author: Vaclav Pech
 * Date: Nov 1, 2009
 */
public abstract class AbstractForkJoinWorker<T> extends RecursiveAction implements ForkJoinWorker<T> {
    private T value;
    private TaskBarrier taskBarrier;

    protected AbstractForkJoinWorker() {
    }

    protected AbstractForkJoinWorker(final TaskBarrier taskBarrier) {
        setTaskBarrier(taskBarrier);
    }

    public final void setTaskBarrier(final TaskBarrier taskBarrier) {
        if (taskBarrier==null) return;
        this.taskBarrier = taskBarrier;
        this.taskBarrier.register();
    }

    protected final void setResult(final T value) {
        this.value = value;
        taskBarrier.arriveAndDeregister();
    }

    public final T getResult() {
        return value;
    }
}
