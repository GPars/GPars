package org.gparallelizer.actors.pooledActors;

import jsr166y.forkjoin.ForkJoinPool;

import java.util.concurrent.*;

/**
 * Represents the actors' thread pool, which performs tasks on behalf of the actors. Uses a ForkJoinPool from JSR-166y
 * The actors' thread pool size defaults to the n + 1, where n is the number of processors/cores available on the machine.
 * The VM parameter -Dgparallelizer.poolsize can be used the configure the default size of the actors' thread pool.
 * The resize() and resetDefaultSize() methods can be used to configure size of the thread pool at runtime.
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public final class FJPool implements Pool {
    private ForkJoinPool pool;

    /**
     * Creates the pool with default number of threads.
     * @param daemon Sets the daemon flag of threads in the pool.
     */
    public FJPool(final boolean daemon) {
        this(daemon, FJPool.retrieveDefaultPoolSize());
    }

    /**
     * Creates the pool with specified number of threads.
     * @param daemon Sets the daemon flag of threads in the pool.
     * @param poolSize The required size of the pool
     */
    public FJPool(final boolean daemon, final int poolSize) {
        if (poolSize<0) throw new IllegalStateException("Pool size must be a non-negative number.");
        pool = createPool(daemon, poolSize);
    }

    /**
     * Creates a fork/join pool of given size. Each thread will have the uncaught exception handler set
     * to print the unhandled exception to standard error output.
     * @param daemon Sets the daemon flag of threads in the pool.
     * @param poolSize The required pool size  @return The created thread pool
     * @return The newly created thread pool
     */
    private ForkJoinPool createPool(final boolean daemon, final int poolSize) {
        //todo use the daemon flag
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
    public void resize(final int poolSize) {
        if (poolSize<0) throw new IllegalStateException("Pool size must be a non-negative number.");
        pool.setPoolSize(poolSize);
    }

    /**
     * Sets the pool size to the default
     */
    public void resetDefaultSize() {
        resize(FJPool.retrieveDefaultPoolSize());
    }

    /**
     * schedules a new task for processing with the pool
     * @param task The task to schedule
     */
    public void execute(final Runnable task) {
//        pool.execute(task);
    }

    /**
     * Retrieves the internal executor service.
     * @return The underlying thread pool
     */
    public ForkJoinPool getForkJoinPool() {
        return pool;
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