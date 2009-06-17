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
                pool.addWorkers(needForThreads);
                System.out.println("Adding workers " + needForThreads + ":" + pool.getPoolSize());
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

                    if (change >= 3) {
                        pool.removeWorkers(change);
                        System.out.println("Removing workers " + change + ":" + pool.getPoolSize());
                    }
                }
            }
        });
    }
}