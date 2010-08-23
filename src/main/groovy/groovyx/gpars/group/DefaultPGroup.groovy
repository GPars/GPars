// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.group

import groovyx.gpars.scheduler.DefaultPool
import groovyx.gpars.scheduler.Pool

/**
 * Provides logical grouping for actors, agents and dataflow tasks and operators. Each group has an underlying thread pool, which will perform actions
 * on behalf of the users belonging to the group. Actors created through the DefaultPGroup.actor() method
 * will automatically belong to the group through which they were created, just like agents created through the agent() or fairAgent() methods
 * or dataflow tasks and operators created through the task() or operator() methods.
 * Uses a pool of non-daemon threads.
 * The DefaultPGroup class implements the Pool interface through @Delegate.
 * <pre>
 *
 * def group = new DefaultPGroup()
 * group.resize 1
 *
 * def actor = group.actor {*     react {message ->
 *         println message
 *}*}.start()
 *
 * actor.send 'Hi!'
 * ...
 * group.shutdown()
 * </pre>
 *
 * Otherwise, if constructing Actors directly through their constructors, the AbstractPooledActor.parallelGroup property,
 * which defaults to the Actors.defaultActorPGroup, can be set before the actor is started.
 *
 * <pre>
 * def group = new DefaultPGroup()
 *
 * def actor = new MyActor()
 * actor.parallelGroup = group
 * actor.start()
 * ...
 * group.shutdown()
 *
 * </pre>
 *
 * @author Vaclav Pech
 * Date: May 4, 2009
 */
public final class DefaultPGroup extends PGroup {

    /**
     * Creates a group for actors, agents, tasks and operators. The actors will share the supplied thread pool.
     * @param threadPool The thread pool to use for the group
     */
    public def DefaultPGroup(final Pool threadPool) {
        super(threadPool)
    }

    /**
     * Creates a group for actors, agents, tasks and operators. The actors will share a common daemon thread pool.
     */
    def DefaultPGroup() {
        super(new DefaultPool(true))
    }

    /**
     * Creates a group for actors, agents, tasks and operators. The actors will share a common daemon thread pool.
     * @param poolSize The initial size of the underlying thread pool
     */
    def DefaultPGroup(final int poolSize) {
        super(new DefaultPool(true, poolSize))
    }
}
