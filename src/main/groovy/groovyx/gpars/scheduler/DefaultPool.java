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

package groovyx.gpars.scheduler;

import groovyx.gpars.util.PoolUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents the actors' thread pool, which performs tasks on behalf of the actors.
 * The actors' thread pool size defaults to the n + 1, where n is the number of processors/cores available on the machine.
 * The VM parameter -Dgpars.poolsize can be used the configure the default size of the actors' thread pool.
 * The resize() and resetDefaultSize() methods can be used to configure size of the thread pool at runtime.
 *
 * @author Vaclav Pech
 *         Date: Feb 27, 2009
 */
public class DefaultPool implements Pool {
    private ThreadPoolExecutor pool;
    private static final long SHUTDOWN_TIMEOUT = 30L;

    /**
     * Creates the pool with default number of daemon threads.
     */
    public DefaultPool() {
        this(true);
    }

    /**
     * Creates the pool with default number of threads.
     *
     * @param daemon Sets the daemon flag of threads in the pool.
     */
    public DefaultPool(final boolean daemon) {
        this(daemon, PoolUtils.retrieveDefaultPoolSize());
    }

    /**
     * Creates the pool with specified number of threads.
     *
     * @param daemon   Sets the daemon flag of threads in the pool.
     * @param poolSize The required size of the pool
     */
    public DefaultPool(final boolean daemon, final int poolSize) {
        if (poolSize < 0) throw new IllegalStateException(Pool.POOL_SIZE_MUST_BE_A_NON_NEGATIVE_NUMBER);
      this.pool = DefaultPool.createPool(daemon, poolSize);
    }

    /**
     * Creates the pool around the given executor service
     *
     * @param pool The executor service to use
     */
    public DefaultPool(final ThreadPoolExecutor pool) {
        this.pool = pool;
    }

    /**
     * Creates a fixed-thread pool of given size. Each thread will have the uncaught exception handler set
     * to print the unhandled exception to standard error output.
     *
     * @param daemon   Sets the daemon flag of threads in the pool.
     * @param poolSize The required pool size  @return The created thread pool
     * @return The newly created thread pool
     */
    private static ThreadPoolExecutor createPool(final boolean daemon, final int poolSize) {
        assert poolSize > 0;
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r, DefaultPool.createThreadName());
                thread.setDaemon(daemon);
                thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(final Thread t, final Throwable e) {
                        System.err.println(Pool.UNCAUGHT_EXCEPTION_OCCURED_IN_ACTOR_POOL + t.getName());
                        e.printStackTrace(System.err);
                    }
                });
                return thread;
            }
        });
    }

    /**
     * Created a JVM-unique name for Actors' threads.
     *
     * @return The name prefix
     */
    protected static String createThreadName() {
        return "Actor Thread " + DefaultPool.threadCount.incrementAndGet();
    }

    /**
     * Unique counter for Actors' threads
     */
    private static final AtomicLong threadCount = new AtomicLong(0L);

    /**
     * Resizes the thread pool to the specified value
     *
     * @param poolSize The new pool size
     */
    public final void resize(final int poolSize) {
        if (poolSize < 0) throw new IllegalStateException(Pool.POOL_SIZE_MUST_BE_A_NON_NEGATIVE_NUMBER);
        pool.setCorePoolSize(poolSize);
    }

    /**
     * Sets the pool size to the default
     */
    public final void resetDefaultSize() {
        resize(PoolUtils.retrieveDefaultPoolSize());
    }

    /**
     * schedules a new task for processing with the pool
     *
     * @param task The task to schedule
     */
    public final void execute(final Runnable task) {
        pool.execute(task);
    }

    /**
     * Retrieves the internal executor service.
     *
     * @return The underlying thread pool
     */
    public final ExecutorService getExecutorService() {
        return this.pool;
    }

    /**
     * Gently stops the pool
     */
    public final void shutdown() {
        pool.shutdown();
        try {
            pool.awaitTermination(DefaultPool.SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();  // set the interrupted flag
        }
    }
}
