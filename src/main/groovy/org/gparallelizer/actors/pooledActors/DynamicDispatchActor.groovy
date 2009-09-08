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
 * A pooled actor allowing for an alternative structure of the message handling code.
 * In general DynamicDispatchActor repeatedly scans for messages and dispatches arrived messages to one
 * of the onMessage(message) methods defined on the actor.
 * <pre>
 * final class MyActor extends DynamicDispatchActor {
 *
 *     void onMessage(String message) {
 *         println 'Received string'
 *     }
 *
 *     void onMessage(Integer message) {
 *         println 'Received integer'
 *     }
 *
 *     void onMessage(Object message) {
 *         println 'Received object'
 *     }
 * }
 * </pre>
 * The dispatch leverages Groovy dynamic method dispatch.
 *
 * @author Vaclav Pech
 * Date: Jun 26, 2009
 */

public abstract class DynamicDispatchActor extends AbstractPooledActor {

    /**
     * Loops reading messages using the react() method and dispatches to the corresponding onMessage() method.
     */
    final void act() {
        loop {
            react {
                onMessage it
            }
        }
    }
}
