package org.gparallelizer.dataflow;

import org.gparallelizer.actors.pooledActors.AbstractPooledActorGroup;
import org.gparallelizer.actors.pooledActors.ResizablePool;

/**
 * Groups all dataflow threads, which are effectively pooled actors.
 * DataFlow leverages a resizable pool of non-daemon threads.
 * DataFlowActorGroup can be used directly to create and group dataflow actors (threads)
 * <pre>
 * DataFlowActorGroup group = new DataFlowActorGroup()
 * group.actor {
 *     ....
 * }
 * </pre>
 *
 * @author Vaclav Pech
 * Date: Jun 21, 2009
 */
public final class DataFlowActorGroup extends AbstractPooledActorGroup  {
   /**
     * Creates a group of pooled actors. The actors will share a common non-daemon thread pool.
     */
    public DataFlowActorGroup() {
        threadPool = new ResizablePool(false);
    }

    /**
     * Creates a group of pooled actors. The actors will share a common non-daemon thread pool.
     * @param poolSize The initial size of the underlying thread pool
     */
    public DataFlowActorGroup(final int poolSize) {
        threadPool = new ResizablePool(false, poolSize);
    }
}
