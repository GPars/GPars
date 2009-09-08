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

/**
 * Registers and deregisters actors with their threads to allow for message-originator retrieval.
 *
 * @author Vaclav Pech
 * Date: Apr 15, 2009
 */
public abstract class ReplyRegistry {

    /**
     * Maps each thread to the actor it currently processes.
     * Used in the send() method to remember the sender of each message for potential replies
     */
    private static ThreadLocal<Actor> currentActorPerThread = new ThreadLocal<Actor>();

    /**
     * Registers the actor with the current thread
     * @param currentActor The actor to register
     */
    public static void registerCurrentActorWithThread(final Actor currentActor) {
        currentActorPerThread.set(currentActor);
    }

    /**
     * Deregisters the actor registered from the thread
     */
    public static void deregisterCurrentActorWithThread() {
        currentActorPerThread.set(null);
    }

    /**
     * Retrieves the actor registerred with the current thread
     * @return The associated actor
     */
    public static Actor threadBoundActor() {
        return currentActorPerThread.get();
    }
}
