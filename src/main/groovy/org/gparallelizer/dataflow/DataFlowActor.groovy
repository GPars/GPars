package org.gparallelizer.dataflow

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActorGroup

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jun 5, 2009
 * Time: 2:07:08 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class DataFlowActor extends AbstractPooledActor {

    public static final PooledActorGroup DATA_FLOW_GROUP = new PooledActorGroup(true, 10)

    def DataFlowActor() {
        this.actorGroup = DATA_FLOW_GROUP
    }
}
