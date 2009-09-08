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

import org.gparallelizer.actors.pooledActors.Pool

/**
 * The common functionality for all actor groups.
 *
 * @author Vaclav Pech
 */
public abstract class AbstractActorGroup {

    /**
     * Stored the group actors' thread pool
     */
    protected @Delegate Pool threadPool

    /**
     * Indicates whether actors in this group use ForkJoin thread pool
     */
    final boolean forkJoinUsed

    def AbstractActorGroup() { this.forkJoinUsed = useFJPool() }

    def AbstractActorGroup(final boolean forkJoinUsed) { this.forkJoinUsed = forkJoinUsed }

    /**
     * Checks the gparallelizer.useFJPool system property and returns, whether or not to use a fork join pool
     */
    protected final boolean useFJPool() {
        return 'true' == System.getProperty("gparallelizer.useFJPool")?.trim()?.toLowerCase()
    }
}
