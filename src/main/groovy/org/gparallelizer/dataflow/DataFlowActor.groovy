package org.gparallelizer.dataflow

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.AbstractPooledActorGroup
import org.gparallelizer.actors.pooledActors.ResizableFJPool
import org.gparallelizer.actors.pooledActors.ResizablePool

/**
 * A parent actor for all actors used in Dataflow Concurrency implementation
 *
 * @author Vaclav Pech
 * Date: Jun 5, 2009
 */
abstract class DataFlowActor extends AbstractPooledActor {

    /**
     * The actor group used by all Dataflow Concurrency actors by default.
     */
    public static final DataFlowActorGroup DATA_FLOW_GROUP = new DataFlowActorGroup(1)

    /**
     * Sets the default Dataflow Concurrency actor group on the actor.
     */
    def DataFlowActor() { this.actorGroup = DATA_FLOW_GROUP }
}
