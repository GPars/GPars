package org.gparallelizer.actors.pooledActors;

/**
 * Represents an actor's thread pool
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public interface Pool {
    /**
     * Resizes the thread pool to the specified value
     * @param poolSize The new pool size
     */
    void resize(int poolSize);

    /**
     * Sets the pool size to the default
     */
    void resetDefaultSize();

    /**
     * schedules a new task for processing with the pool
     * @param task The task to schedule
     */
    void execute(Runnable task);

    /**
     * Gently stops the pool
     */
    void shutdown();
}
