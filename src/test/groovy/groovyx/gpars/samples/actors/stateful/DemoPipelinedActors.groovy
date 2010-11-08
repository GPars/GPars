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

package groovyx.gpars.samples.actors.stateful

import groovyx.gpars.actor.Actors

/**
 * Creates a chain of actors, which pass a message from one end of the chain to the other one.
 */

final intermediaries = []

def src = Actors.actor {index ->
    println "Source: Starting..."
    react {go ->
        println "Source: got '$go'; sending 'hello'.."
        println "Source: got '${intermediaries[1].sendAndWait('hello')}'"
    }
}

intermediaries << src

for (i in 1..3) {
    intermediaries << Actors.actor({index ->
        println "Intermediary[$index]: Starting..."

        react {msg ->
            println("Intermediary[$index]: Received: '$msg'")
            reply intermediaries[index + 1].sendAndWait("${msg}[$index]")
        }
    }.curry(i))

}

intermediaries << Actors.actor {
    println "Sink: Starting..."
    react {msg ->
        println "Sink: got '$msg'"
        reply "thanks"
    }
}

src << 'go'

sleep 3000
"OK"
