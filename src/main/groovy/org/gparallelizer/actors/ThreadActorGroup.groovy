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

package org.gparallelizer.actors

import org.gparallelizer.actors.pooledActors.ResizableFJPool
import org.gparallelizer.actors.pooledActors.ResizablePool

/**
 * Provides logical grouping for actors. Each group holds a thread pool, which will provide threads for all
 * actors created in that group. Actors created through the ActorGroup.actor*() methods will automatically belong
 * to the group through which they were created.
 * <pre>
 *
 * def group = new ThreadActorGroup()
 *
 * def actor = group.actor {
 *     receive {message ->
 *         println message
 *     }
 * }.start()
 *
 * actor.send 'Hi!'
 * ...
 * actor.stop()
 * </pre>
 *
 * Otherwise, if constructing Actors directly through their constructors, the CommonActorImpl.actorGroup property,
 * which defaults to the Actors.defaultActorGroup, can be set before the actor is started.
 *
 * <pre>
 * def group = new ThreadActorGroup(false)
 *
 * def actor = new MyActor()
 * actor.actorGroup = group
 * actor.start()
 *
 * </pre>
 *
 * To specify whether a ForkJoinPool from JSR-166y should be used or the pool based on JDK's executor services,
 * you can either use the appropriate constructors or the 'gparallelizer.useFJPool' system property.
 *
 * ThreadActorGroups use pools of daemon threads.
 *
 * @author Vaclav Pech
 * Date: Jun 17, 2009
 */
public final class ThreadActorGroup extends AbstractThreadActorGroup {

    /**
     * Creates a group of actors. The actors will share a common thread pool of threads.
     */
    protected def ThreadActorGroup() {
        threadPool = forkJoinUsed ? new ResizableFJPool() : new ResizablePool(true)
    }

    /**
     * Creates a group of actors. The actors will share a common thread pool.
     * @param useForkJoinPool Indicates, whether the group should use a fork join pool underneath or the executor-service-based default pool
     */
    protected def ThreadActorGroup(final boolean useForkJoinPool) {
        super(useForkJoinPool)
        threadPool = forkJoinUsed ? new ResizableFJPool() : new ResizablePool(true)
    }

    /**
     * Creates a group of actors. The actors will share a common thread pool.
     * @param poolSize The initial size of the underlying thread pool
     */
    protected def ThreadActorGroup(final int poolSize) {
        threadPool = forkJoinUsed ? new ResizableFJPool(poolSize) : new ResizablePool(true, poolSize)
    }

    /**
     * Creates a group of actors. The actors will share a common thread pool.
     * @param poolSize The initial size of the underlying thread pool
     * @param useForkJoinPool Indicates, whether the group should use a fork join pool underneath or the executor-service-based default pool
     */
    protected def ThreadActorGroup(final int poolSize, final boolean useForkJoinPool) {
        super(useForkJoinPool)
        threadPool = forkJoinUsed ? new ResizableFJPool(poolSize) : new ResizablePool(true, poolSize)
    }
}
