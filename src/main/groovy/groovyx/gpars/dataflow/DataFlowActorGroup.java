//  GPars (formerly GParallelizer)
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

package groovyx.gpars.dataflow;

import groovyx.gpars.actor.ActorGroup;
import groovyx.gpars.scheduler.ResizeablePool;

/**
 * Groups all dataflow threads, which are effectively pooled actors.
 * DataFlow leverages a resizeable pool of non-daemon threads.
 * DataFlowActorGroup can be used directly to create and group dataflow actors (threads)
 * <pre>
 * DataFlowActorGroup group = new DataFlowActorGroup()
 * group.actor {
 *     ....
 * }
 * </pre>
 *
 * @author Vaclav Pech, Alex Tkachman
 *         Date: Jun 21, 2009
 */
public final class DataFlowActorGroup extends ActorGroup {
    /**
     * Creates a group of pooled actors. The actors will share a common non-daemon thread pool.
     *
     * @param poolSize The initial size of the underlying thread pool
     */
    public DataFlowActorGroup(final int poolSize) {
        super(new ResizeablePool(false, poolSize));
    }
}
