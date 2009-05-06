package org.gparallelizer.actors.pooledActors;

import java.util.concurrent.*;

/**
 * Represents the actors' thread pool, which performs tasks on behalf of the actors.
 * The actors' thread pool size defaults to the n + 1, where n is the number of processors/cores available on the machine.
 * The VM parameter -Dgparallelizer.poolsize can be used the configure the default size of the actors' thread pool.
 * The resize() and resetDefaultSize() methods can be used to configure size of the thread pool at runtime.
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public final class DefaultPool implements Pool {
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
        createPool(daemon, poolSize);
    }

    /**
     * Creates a fixed-thread pool of given size. Each thread will have the uncaught exception handler set
     * to print the unhandled exception to standard error output.
     * @param daemon Sets the daemon flag of threads in the pool.
     * @param poolSize The required pool size  @return The created thread pool
     * @return The newly created thread pool
     */
    private ExecutorService createPool(final boolean daemon, final int poolSize) {
        assert poolSize > 0;
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r);
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
        return pool;
    }

    /**
     * Resizes the thread pool to the specified value
     * @param poolSize The new pool size
     */
    public void resize(final int poolSize) {
        if (poolSize<0) throw new IllegalStateException("Pool size must be a non-negative number.");
        pool.setCorePoolSize(poolSize);
    }

    /**
     * Sets the pool size to the default
     */
    public void resetDefaultSize() {
        resize(DefaultPool.retrieveDefaultPoolSize());
    }

    /**
     * schedules a new task for processing with the pool
     * @param task The task to schedule
     */
    public void execute(final Runnable task) {
        pool.execute(task);
    }

    /**
     * Gently stops the pool
     */
    public void shutdown() {
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
