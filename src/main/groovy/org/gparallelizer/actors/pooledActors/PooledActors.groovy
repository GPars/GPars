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
 * Provides handy helper methods to create pooled actors and customize the underlying thread pool.
 * Use static import to be able to call PooledActors methods without the need to prepend them with the PooledActors identifier.
 * <pre>
 * import static org.gparallelizer.actors.pooledActors.PooledActors.*
 *
 * PooledActors.defaultPooledActorGroup.resize 1
 *
 * def actor = actor {
 *     react {message ->
 *         println message
 *     }
 *     //this line will never be reached
 * }.start()
 *
 * actor.send 'Hi!'
 * </pre>
 *
 * All actors created through the PooledActors class will belong to the same default actor group and run
 * on daemon threads.
 * The PooledActorGroup class should be used when actors need to be grouped into multiple groups or when non-daemon
 * threads are to be used.
 * @author Vaclav Pech
 * Date: Feb 18, 2009
 */
public abstract class PooledActors {

    /**
     * The default actor group to share by all actors created through the PooledActors class.
     */
    public final static PooledActorGroup defaultPooledActorGroup = new PooledActorGroup()

    /**
     * Creates a new instance of PooledActor, using the passed-in closure as the body of the actor's act() method.
     * The created actor will be part of the default actor group.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the AbstractPooledActor class
     */
    public static AbstractPooledActor actor(Closure handler) {
        return defaultPooledActorGroup.actor(handler)
    }

    /**
     * Creates a reactor around the supplied code.
     * When a reactor receives a message, the supplied block of code is run with the message
     * as a parameter and the result of the code is send in reply.
     * The created actor will be part of the default actor group.
     * @param The code to invoke for each received message
     * @return A new instance of ReactiveEventBasedThread
     */
    public static AbstractPooledActor reactor(final Closure code) {
        return defaultPooledActorGroup.reactor(code)
    }
}
