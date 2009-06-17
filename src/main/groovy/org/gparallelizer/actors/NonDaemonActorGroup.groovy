package org.gparallelizer.actors

import org.gparallelizer.actors.AbstractThreadActorGroup
import org.gparallelizer.actors.pooledActors.ResizablePool

/**
 * Represents a group of thread-bound actors, which share a pool of non-daemon threads. Since Fork/Join doesn't support
 * non-daemon threads, the pool for NonDaemonActorGroup will always use JDK ExecutorService pools.
* @see org.gparallelizer.actors.ThreadActorGroup for more details on groups of pooled actors.
 *
 * @author Vaclav Pech
 * Date: Jun 17, 2009
 */
public final class NonDaemonActorGroup extends AbstractThreadActorGroup {

    /**
     * Creates a group of actors. The actors will share a common non-daemon thread pool.
     */
    def NonDaemonActorGroup() {
        threadPool = new ResizablePool(false)
    }

    /**
     * Creates a group of actors. The actors will share a common non-daemon thread pool.
     * @param poolSize The initial size of the underlying thread pool
     */
    def NonDaemonActorGroup(final int poolSize) {
        threadPool = new ResizablePool(false, poolSize)
    }

    /**
     * Fork/Join not used by this group
     */
    boolean getUsedForkJoin() { false }
}