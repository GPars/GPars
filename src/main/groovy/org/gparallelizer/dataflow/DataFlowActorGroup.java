package org.gparallelizer.dataflow;

import org.gparallelizer.actors.pooledActors.AbstractPooledActorGroup;
import org.gparallelizer.actors.pooledActors.ResizableFJPool;
import org.gparallelizer.actors.pooledActors.ResizablePool;

/**
 * Groups all dataflow threads, which are effectively pooled actors.
 *
 * @author Vaclav Pech
 * Date: Jun 21, 2009
 */
public class DataFlowActorGroup extends AbstractPooledActorGroup  {
   /**
     * Creates a group of pooled actors. The actors will share a common daemon thread pool.
     */
    public DataFlowActorGroup() {
        threadPool = isForkJoinUsed() ? new ResizableFJPool() : new ResizablePool(false);
    }

    /**
     * Creates a group of pooled actors. The actors will share a common daemon thread pool.
     * @param useForkJoinPool Indicates, whether the group should use a fork join pool underneath or the executor-service-based default pool
     */
    public DataFlowActorGroup(final boolean useForkJoinPool) {
        super(useForkJoinPool);
        threadPool = isForkJoinUsed() ? new ResizableFJPool() : new ResizablePool(false);
    }

    /**
     * Creates a group of pooled actors. The actors will share a common daemon thread pool.
     * @param poolSize The initial size of the underlying thread pool
     */
    public DataFlowActorGroup(final int poolSize) {
        threadPool = isForkJoinUsed() ? new ResizableFJPool(poolSize) : new ResizablePool(false, poolSize);
    }

    /**
     * Creates a group of pooled actors. The actors will share a common daemon thread pool.
     * @param poolSize The initial size of the underlying thread pool
     * @param useForkJoinPool Indicates, whether the group should use a fork join pool underneath or the executor-service-based default pool
     */
    public DataFlowActorGroup(final int poolSize, final boolean useForkJoinPool) {
        super(useForkJoinPool);
        threadPool = isForkJoinUsed() ? new ResizableFJPool(poolSize) : new ResizablePool(false, poolSize);
    }
}
