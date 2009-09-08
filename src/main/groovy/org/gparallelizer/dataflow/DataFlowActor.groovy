//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

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
