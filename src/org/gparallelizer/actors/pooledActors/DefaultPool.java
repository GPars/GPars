package org.gparallelizer.actors.pooledActors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public class DefaultPool implements Pool {
    private ThreadPoolExecutor pool;

    public DefaultPool() {
        this(Runtime.getRuntime().availableProcessors() + 1);
    }

    public DefaultPool(final int poolSize) {
        createPool(poolSize);
    }

    private ExecutorService createPool(final int poolSize) {
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);

        //todo set exception handler
        //todo parametrize
        return pool;
    }

    public void initialize(final int size) {
        pool.setCorePoolSize(size);
    }

    //todo readress the parameter type
    public void execute(final Object task) {
        pool.execute((Runnable) task);
    }

    public void shutdown() {
        pool.shutdown();
    }
}
