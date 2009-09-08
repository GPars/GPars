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

package org.gparallelizer.actors.pooledActors;

/**
 * Represents an actor's thread pool
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public interface Pool {
    /**
     * Resizes the thread pool to the specified value
     * @param poolSize The new pool size
     */
    void resize(int poolSize);

    /**
     * Sets the pool size to the default
     */
    void resetDefaultSize();

    /**
     * schedules a new task for processing with the pool
     * @param task The task to schedule
     */
    void execute(Runnable task);

    /**
     * Gently stops the pool
     */
    void shutdown();
}
