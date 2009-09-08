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

import jsr166y.forkjoin.ForkJoinPool;

import java.util.concurrent.TimeUnit;

/**
 * Represents the actors' thread pool, which performs tasks on behalf of the actors. Uses a ForkJoinPool from JSR-166y
 * The actors' thread pool size defaults to the n + 1, where n is the number of processors/cores available on the machine.
 * The VM parameter -Dgparallelizer.poolsize can be used the configure the default size of the actors' thread pool.
 * The resize() and resetDefaultSize() methods can be used to configure size of the thread pool at runtime.
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public class FJPool implements Pool {
    protected ForkJoinPool pool;
    private final int configuredPoolSize;

    /**
     * Creates the pool with default number of threads.
     */
    public FJPool() {
        this(FJPool.retrieveDefaultPoolSize());
    }

    /**
     * Creates the pool with specified number of threads.
     * @param configuredPoolSize The required size of the pool
     */
    public FJPool(final int configuredPoolSize) {
        if (configuredPoolSize <0) throw new IllegalStateException("Pool size must be a non-negative number.");
        this.configuredPoolSize = configuredPoolSize;
        pool = createPool(configuredPoolSize);
    }

    /**
     * Creates a fork/join pool of given size. Each thread will have the uncaught exception handler set
     * to print the unhandled exception to standard error output.
     * @param poolSize The required pool size  @return The created thread pool
     * @return The newly created thread pool
     */
    private ForkJoinPool createPool(final int poolSize) {
        assert poolSize > 0;

        final ForkJoinPool pool =  new ForkJoinPool(poolSize);
        pool.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(final Thread t, final Throwable e) {
                System.err.println("Uncaught exception occured in actor pool " + t.getName());
                e.printStackTrace(System.err);
            }
        });
        return pool;
    }

    /**
     * Resizes the thread pool to the specified value
     * @param poolSize The new pool size
     */
    public final void resize(final int poolSize) {
        if (poolSize<0) throw new IllegalStateException("Pool size must be a non-negative number.");
        pool.setPoolSize(poolSize);
    }

    /**
     * Sets the pool size to the default
     */
    public final void resetDefaultSize() {
        resize(FJPool.retrieveDefaultPoolSize());
    }

    /**
     * schedules a new task for processing with the pool
     * @param task The task to schedule
     */
    public void execute(final Runnable task) {
        pool.submit(new FJRunnableTask(task));
    }

    /**
     * Retrieves the internal executor service.
     * @return The underlying thread pool
     */
    public final ForkJoinPool getForkJoinPool() {
        return pool;
    }

    /**
     * Gently stops the pool
     */
    public final void shutdown() {
        pool.shutdown();
        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // set the interrupted flag
        }
    }

    private static int retrieveDefaultPoolSize() {
        final String poolSizeValue = System.getProperty("gparallelizer.poolsize");
        try {
            return Integer.parseInt(poolSizeValue);
        } catch (NumberFormatException e) {
            return Runtime.getRuntime().availableProcessors() + 1;
        }
    }

    protected final int getConfiguredPoolSize() { return configuredPoolSize; }
}
