package org.gparallelizer.actors.pooledActors

import jsr166y.forkjoin.ForkJoinPool
import java.util.concurrent.TimeUnit
import jsr166y.forkjoin.ForkJoinTask

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Feb 7, 2009
 */
public final class Pool {

    private final ForkJoinPool pool;

    def Pool() {
        this(Runtime.getRuntime().availableProcessors() + 1)
    }

    def Pool(final int poolSize) {
        createPool(poolSize)
    }

    private final ForkJoinPool createPool(final int poolSize) {
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

    public final <T> void execute(ForkJoinTask<T> task) {
        pool.execute task
    }

    public final initialize(int size) {
        pool.poolSize = size
    }
}