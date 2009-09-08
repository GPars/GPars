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

/**
 * Represents a group of pooled-bound actors, which share a pool of non-daemon threads. Since Fork/Join doesn't support
 * non-daemon threads, the pool for NonDaemonPooledActorGroup will always use JDK ExecutorService pools.
 * @see org.gparallelizer.actors.pooledActors.PooledActorGroup for more details on groups of pooled actors.
 *
 * @author Vaclav Pech
 * Date: Jun 17, 2009
 */
public final class NonDaemonPooledActorGroup extends AbstractPooledActorGroup {

    /**
     * Creates a group of pooled actors. The actors will share a common non-daemon thread pool.
     */
    def NonDaemonPooledActorGroup() {
        super(false)
        threadPool = new DefaultPool(false)
    }

    /**
     * Creates a group of pooled actors. The actors will share a common non-daemon thread pool.
     * @param poolSize The initial size of the underlying thread pool
     */
    def NonDaemonPooledActorGroup(final int poolSize) {
        super(false)
        threadPool = new DefaultPool(false, poolSize)
    }

    /**
     * Fork/Join not used by this group
     */
    boolean getUsedForkJoin() { false }
}
