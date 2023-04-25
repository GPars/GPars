/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package groovyx.gpars.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Represents a pool backed by any executor service.
 * This pool has less configuration options than DefaultPool.
 */
public class ExecutorPool implements Pool {
    private final ExecutorService pool;
    private static final long SHUTDOWN_TIMEOUT = 30L;

    /**
     * Creates the pool using the given ExecutorService.
     *
     * @param pool The ExecutorService to use for the pool.
     */
    public ExecutorPool(final ExecutorService pool) {
        this.pool = pool;
    }

    /**
     * Resizes the thread pool to the specified value
     *
     * @param poolSize The new pool size
     */
    @Override
    public final void resize(final int poolSize) {
        throw new UnsupportedOperationException("resize not supported for ExecutorPool, avoid this method or consider using DefaultPool");
    }

    /**
     * Sets the pool size to the default
     */
    @Override
    public final void resetDefaultSize() {
        throw new UnsupportedOperationException("resetDefaultSize not supported for ExecutorPool, avoid this method or consider using DefaultPool");
    }

    /**
     * Retrieves the current thread pool size
     *
     * @return The pool size
     */
    @Override
    public int getPoolSize() {
        throw new UnsupportedOperationException("getPoolSize not supported for ExecutorPool, avoid this method or consider using DefaultPool");
    }

    /**
     * schedules a new task for processing with the pool
     *
     * @param task The task to schedule
     */
    @Override
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
    @Override
    public final void shutdown() {
        pool.shutdown();
        try {
            pool.awaitTermination(ExecutorPool.SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();  // set the interrupted flag
        }
    }
}
