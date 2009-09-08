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
 * Provides a common super class fo pooled actor's groups.
 *
 * @author Vaclav Pech
 * Date: May 8, 2009
 */
public abstract class AbstractPooledActorGroup extends AbstractActorGroup {

    def AbstractPooledActorGroup() { }

    def AbstractPooledActorGroup(final boolean usedForkJoin) { super(usedForkJoin); }

    /**
     * Creates a new instance of PooledActor, using the passed-in runnable/closure as the body of the actor's act() method.
     * The created actor will belong to the pooled actor group.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the AbstractPooledActor class
     */
    public final AbstractPooledActor actor(Runnable handler) {
        final AbstractPooledActor actor = new RunnableBackedPooledActor (handler)
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a reactor around the supplied code.
     * When a reactor receives a message, the supplied block of code is run with the message
     * as a parameter and the result of the code is send in reply.
     * @param The code to invoke for each received message
     * @return A new instance of ReactiveEventBasedThread
     */
    public final AbstractPooledActor reactor(final Closure code) {
        new ReactiveActor(code)
    }
}
