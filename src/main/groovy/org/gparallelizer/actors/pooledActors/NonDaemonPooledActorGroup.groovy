package org.gparallelizer.actors.pooledActors

/**
 * Represents a group of pooled-bound actors, which share a pool of non-daemon threads. Since Fork/Join doesn't support
 * non-daemon threads, the pool for NonDaemonPooledActorGroup will always use JDK ExecutorService pools.
 * @see org.gparallelizer.actors.pooledActors.PooledActorGroup for more details on groups of pooled actors.
 *
 * @author Vaclav Pech
 * Date: Jun 17, 2009
 */
public final class NonDaemonPooledActorGroup extends AbstractPooledActorGroup {

    /**
     * Creates a group of pooled actors. The actors will share a common non-daemon thread pool.
     */
    def NonDaemonPooledActorGroup() {
        threadPool = new DefaultPool(false)
    }

    /**
     * Creates a group of pooled actors. The actors will share a common non-daemon thread pool.
     * @param poolSize The initial size of the underlying thread pool
     */
    def NonDaemonPooledActorGroup(final int poolSize) {
        threadPool = new DefaultPool(false, poolSize)
    }

    /**
     * Fork/Join not used by this group
     */
    boolean getUsedForkJoin() { false }
}