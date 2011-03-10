// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.samples.userguide.actor

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors

/**
 * @author Vaclav Pech
 */

Actor demoActor = Actors.actor {
    delegate.metaClass {
        handleA = {->
            react {a ->
                handleB(a)
            }
        }

        handleB = {a ->
            react {b ->
                println a + b
                reply a + b
            }
        }
    }

    handleA()
}

Actors.actor {
    demoActor 10
    demoActor 20
    react {
        println "Result: $it"
    }
}.join()
