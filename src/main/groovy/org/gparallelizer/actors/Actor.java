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
package org.gparallelizer.actors;

import org.gparallelizer.MessageStream;
import org.gparallelizer.remote.RemoteActor;

/**
 * Actors are active objects, which either have their own thread processing repeatedly messages submitted to them
 * or they borrow a thread from a thread pool. Actors implementing the ThreadedActor interface have their own thread,
 * whereas actors implementing PooledActor interface delegate processing to threads from a thread pool.
 * The Actor interface provides means to send messages to the actor, start and stop the background thread as well as
 * check its status.
 *
 * @author Vaclav Pech, Alex Tkachman
 */
public abstract class Actor extends MessageStream {

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     * @return same actor
     */
    public abstract Actor start();

    /**
     * Stops the Actor. Unprocessed messages will be passed to the afterStop method, if exists.
     * Has no effect if the Actor is not started.
     * @return same actor
     */
    public abstract Actor stop();

    /**
     * Checks the current status of the Actor.
     * @return current status of the Actor.
     */
    public abstract boolean isActive();

    /**
     * Checks whether the current thread is the actor's current thread.
     * @return whether the current thread is the actor's current thread
     */
    public abstract boolean isActorThread();

    /**
     * Joins the actor. Waits for its termination.
     */
    public abstract void join();

    /**
     * Joins the actor. Waits fot its termination.
     * @param milis Timeout in miliseconds
     */
    public abstract void join(long milis);

    public Class getRemoteClass() {
        return RemoteActor.class;
    }
}
