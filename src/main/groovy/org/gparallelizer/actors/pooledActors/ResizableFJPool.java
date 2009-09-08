//  GParallelizer
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

package org.gparallelizer.actors.pooledActors;

/**
 * Represents the actors' thread pool, which performs tasks on behalf of the actors. Uses a ForkJoinPool from JSR-166y
 * The actors' thread pool size defaults to the n + 1, where n is the number of processors/cores available on the machine.
 * The VM parameter -Dgparallelizer.poolsize can be used the configure the default size of the actors' thread pool.
 * The resize() and resetDefaultSize() methods can be used to configure size of the thread pool at runtime.
 *
 * @author Vaclav Pech
 *         Date: Feb 27, 2009
 */
public final class ResizableFJPool extends FJPool {
    private static final int MAX_POOL_SIZE = 1000;

    /**
     * Creates the pool with default number of threads.
     */
    public ResizableFJPool() {
        super();
    }

    /**
     * Creates the pool with specified number of threads.
     *
     * @param poolSize The required size of the pool
     */
    public ResizableFJPool(final int poolSize) {
        super(poolSize);
    }

    /**
     * schedules a new task for processing with the pool
     *
     * @param task The task to schedule
     */
    @Override public void execute(final Runnable task) {
        synchronized (this) {
            final int currentPoolSize = pool.getPoolSize();
            final int submissionCount = pool.getActiveSubmissionCount();
            final int needForThreads = submissionCount + 1 - currentPoolSize;
            if (needForThreads > 0) {
                if (currentPoolSize + needForThreads> ResizableFJPool.MAX_POOL_SIZE) {
                    throw new IllegalStateException("The thread pool executor cannot run the task. " +
                            "The upper limit of the thread pool size has probably been reached. " +
                            "Current pool size: " + currentPoolSize + " Maximum pool size: " + ResizableFJPool.MAX_POOL_SIZE);
                }
                pool.addWorkers(needForThreads);
            }
        }
        super.execute(new Runnable() {
            public void run() {
                task.run();
                synchronized (ResizableFJPool.this) {
                    final int currentPoolSize = pool.getPoolSize();
                    final int submissionCount = pool.getActiveSubmissionCount();
                    final int desiredPoolSize = Math.max(submissionCount, getConfiguredPoolSize());
                    final int change = currentPoolSize - desiredPoolSize;

                    if (change >= 3) pool.removeWorkers(change); }
            }
        });
    }
}
