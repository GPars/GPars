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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents the actors' thread pool, which performs tasks on behalf of the actors.
 * The actors' thread pool size defaults to the n + 1, where n is the number of processors/cores available on the machine.
 * The VM parameter -Dgparallelizer.poolsize can be used the configure the default size of the actors' thread pool.
 * The resize() and resetDefaultSize() methods can be used to configure size of the thread pool at runtime.
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public class DefaultPool implements Pool {
    private ThreadPoolExecutor pool;

    /**
     * Creates the pool with default number of threads.
     * @param daemon Sets the daemon flag of threads in the pool.
     */
    public DefaultPool(final boolean daemon) {
        this(daemon, DefaultPool.retrieveDefaultPoolSize());
    }

    /**
     * Creates the pool with specified number of threads.
     * @param daemon Sets the daemon flag of threads in the pool.
     * @param poolSize The required size of the pool
     */
    public DefaultPool(final boolean daemon, final int poolSize) {
        if (poolSize<0) throw new IllegalStateException("Pool size must be a non-negative number.");
        pool = createPool(daemon, poolSize);
    }

    /**
     * Creates a fixed-thread pool of given size. Each thread will have the uncaught exception handler set
     * to print the unhandled exception to standard error output.
     * @param daemon Sets the daemon flag of threads in the pool.
     * @param poolSize The required pool size  @return The created thread pool
     * @return The newly created thread pool
     */
    protected ThreadPoolExecutor createPool(final boolean daemon, final int poolSize) {
        assert poolSize > 0;
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r, createThreadName());
                thread.setDaemon(daemon);
                thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(final Thread t, final Throwable e) {
                        System.err.println("Uncaught exception occured in actor pool " + t.getName());
                        e.printStackTrace(System.err);
                    }
                });
                return thread;
            }
        });
    }

    /**
     * Created a JVM-unique name for Actors' threads.
     * @return The name prefix
     */
    protected final String createThreadName() {
        return "Actor Thread " + DefaultPool.threadCount.incrementAndGet();
    }

    /**
     * Unique counter for Actors' threads
     */
    private static final AtomicLong threadCount = new AtomicLong(0);

    /**
     * Resizes the thread pool to the specified value
     * @param poolSize The new pool size
     */
    public final void resize(final int poolSize) {
        if (poolSize<0) throw new IllegalStateException("Pool size must be a non-negative number.");
        pool.setCorePoolSize(poolSize);
    }

    /**
     * Sets the pool size to the default
     */
    public final void resetDefaultSize() {
        resize(DefaultPool.retrieveDefaultPoolSize());
    }

    /**
     * schedules a new task for processing with the pool
     * @param task The task to schedule
     */
    public final void execute(final Runnable task) {
        pool.execute(task);
    }

    /**
     * Retrieves the internal executor service.
     * @return The underlying thread pool
     */
    public final ExecutorService getExecutorService() {
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
}
