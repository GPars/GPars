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
public final class ResizablePool extends DefaultPool {

    /**
     * Creates the pool with default number of threads.
     * @param daemon Sets the daemon flag of threads in the pool.
     */
    public ResizablePool(final boolean daemon) {
        super(daemon);
    }

    /**
     * Creates the pool with specified number of threads.
     * @param daemon Sets the daemon flag of threads in the pool.
     * @param poolSize The required size of the pool
     */
    public ResizablePool(final boolean daemon, final int poolSize) {
        super(daemon, poolSize);
    }

    /**
     * Creates a fixed-thread pool of given size. Each thread will have the uncaught exception handler set
     * to print the unhandled exception to standard error output.
     * @param daemon Sets the daemon flag of threads in the pool.
     * @param poolSize The required pool size  @return The created thread pool
     * @return The newly created thread pool
     */
    @Override protected ThreadPoolExecutor createPool(final boolean daemon, final int poolSize) {
        assert poolSize > 0;
         return new ThreadPoolExecutor(poolSize, 100*poolSize, 10, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
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
}