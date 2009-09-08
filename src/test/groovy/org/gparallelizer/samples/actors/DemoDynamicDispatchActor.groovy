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

package org.gparallelizer.samples.actors

import org.gparallelizer.actors.pooledActors.DynamicDispatchActor

/**
 * Demonstrates use of the DynamicDispatchActor class, which leverages Groovy dynamic method dispatch to invoke
 * the appropriate onMessage() method.
 */

final class MyActor extends DynamicDispatchActor {
    void onMessage(String message) {
        println 'Received string'
    }

    void onMessage(Integer message) {
        println 'Received integer'
    }

    void onMessage(Object message) {
        println 'Received object'
    }

    void onMessage(List message) {
        println 'Received list'
        stop()
    }
}

final def actor = new MyActor().start()

actor  << 1
actor  << ''
actor  << 1.0
actor  << new ArrayList()

actor.join()
