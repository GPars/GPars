// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.actors.dda

import groovyx.gpars.actor.DynamicDispatchActor

/**
 * Demonstrates use of the DynamicDispatchActor class, which leverages Groovy dynamic method dispatch to invoke
 * the appropriate onMessage() method.
 */

final class MyActor extends DynamicDispatchActor {

    def MyActor(final closure) { become(closure); }

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

final def actor = new MyActor({
    when {BigDecimal num -> println 'Received BigDecimal'}
}).start()

actor 1
actor ''
actor 1.0
actor([1, 2, 3, 4, 5])

actor.join()
