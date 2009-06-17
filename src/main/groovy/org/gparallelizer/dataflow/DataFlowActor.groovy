package org.gparallelizer.dataflow

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActorGroup

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
    public static final PooledActorGroup DATA_FLOW_GROUP = new PooledActorGroup(50)

    /**
     * Sets the default Dataflow Concurrency actor group on the actor.
     */
    def DataFlowActor() {
        this.actorGroup = DATA_FLOW_GROUP
    }
}
