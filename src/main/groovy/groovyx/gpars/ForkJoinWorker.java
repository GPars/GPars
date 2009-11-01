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

import jsr166y.forkjoin.TaskBarrier;

/**
 * Specifies the contract between the ForkJoinOrchestrator and the workers.
 *
 * Author: Vaclav Pech
 * Date: Nov 1, 2009
 */
public interface ForkJoinWorker<T> {
    /**
     * Sets the parent barrier which it waits on for all workers to finish their work.
     * The worker is supposed to register to the barrier and call arriveAndDeregister() once it has completed its work.
     * @param taskBarrier The barrier to use for communication with the parent
     */
    void setTaskBarrier(TaskBarrier taskBarrier);

    /**
     * Retrieves the result of the worker
     * @return The value calculated by the worker
     */
    T getResult() throws InterruptedException;
}
