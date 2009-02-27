package org.gparallelizer.actors.pooledActors

import java.util.concurrent.TimeUnit
import jsr166y.forkjoin.ForkJoinPool
import org.gparallelizer.actors.pooledActors.Pool

/**
 *
 * @author Vaclav Pech
 * Date: Feb 7, 2009
 */
public final class FJBasedPool implements Pool {

    private final ForkJoinPool pool;

    def FJBasedPool() {
        this(Runtime.getRuntime().availableProcessors() + 1)
    }

    def FJBasedPool(final int poolSize) {
        createPool(poolSize)
    }

    private ForkJoinPool createPool(final int poolSize) {
        pool = new ForkJoinPool(poolSize);

        //todo parametrize
        pool.setUncaughtExceptionHandler({final Thread t, final Throwable e ->
            System.out.println("t = " + t); e.printStackTrace();
        } as Thread.UncaughtExceptionHandler)
        return pool;
    }

    public final void shutdown() {
        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.MINUTES)
    }

    public final void execute(Object task) {
        pool.execute task
    }

    public final void initialize(int size) {
        pool.poolSize = size
    }
}