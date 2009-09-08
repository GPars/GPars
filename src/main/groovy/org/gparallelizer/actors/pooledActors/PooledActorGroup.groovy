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

package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.AbstractActorGroup

/**
 * Provides logical grouping for pooled actors. Each group has an underlying thread pool, which will perform actions
 * on behalf of the actors belonging to the group. Actors created through the PooledActorGroup.actor() method
 * will automatically belong to the group through which they were created.
 * The PooledActorGroup class implements the Pool interface through @Delegate.
 * <pre>
 *
 * def group = new PooledActorGroup()
 * group.resize 1
 *
 * def actor = group.actor {
 *     react {message ->
 *         println message
 *     }
 * }.start()
 *
 * actor.send 'Hi!'
 * ...
 * group.shutdown()
 * </pre>
 *
 * Otherwise, if constructing Actors directly through their constructors, the AbstractPooledActor.actorGroup property,
 * which defaults to the PooledActors.defaultPooledActorGroup, can be set before the actor is started.
 *
 * <pre>
 * def group = new PooledActorGroup(false)
 *
 * def actor = new MyActor()
 * actor.actorGroup = group
 * actor.start()
 * ...
 * group.shutdown()
 *
 * </pre>
 *
 * To specify whether a ForkJoinPool from JSR-166y should be used or the pool based on JDK's executor services,
 * you can either use the appropriate constructors or the 'gparallelizer.useFJPool' system property.
 *
 * PooledActorGroups use pools of daemon threads.
 *
 * @author Vaclav Pech
 * Date: May 4, 2009
 */
public final class PooledActorGroup extends AbstractPooledActorGroup {

    /**
     * Creates a group of pooled actors. The actors will share a common daemon thread pool.
     */
    def PooledActorGroup() {
        threadPool = forkJoinUsed ? new FJPool() : new DefaultPool(true)
    }

    /**
     * Creates a group of pooled actors. The actors will share a common daemon thread pool.
     * @param useForkJoinPool Indicates, whether the group should use a fork join pool underneath or the executor-service-based default pool
     */
    def PooledActorGroup(final boolean useForkJoinPool) {
        super(useForkJoinPool)
        threadPool = forkJoinUsed ? new FJPool() : new DefaultPool(true)
    }

    /**
     * Creates a group of pooled actors. The actors will share a common daemon thread pool.
     * @param poolSize The initial size of the underlying thread pool
     */
    def PooledActorGroup(final int poolSize) {
        threadPool = forkJoinUsed ? new FJPool(poolSize) : new DefaultPool(true, poolSize)
    }

    /**
     * Creates a group of pooled actors. The actors will share a common daemon thread pool.
     * @param poolSize The initial size of the underlying thread pool
     * @param useForkJoinPool Indicates, whether the group should use a fork join pool underneath or the executor-service-based default pool
     */
    def PooledActorGroup(final int poolSize, final boolean useForkJoinPool) {
        super(useForkJoinPool)
        threadPool = forkJoinUsed ? new FJPool(poolSize) : new DefaultPool(true, poolSize)
    }
}
