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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataflowVariable
import java.util.concurrent.TimeUnit
import static groovyx.gpars.dataflow.Dataflow.task

/**
 * Demonstrates that deadlocks are deterministic in dataflow concurrency model. The deadlock appears reliably every time
 * the sample is run.
 * Also shows a way to exchange messages among threads and set and handle timeouts.
 */

final def a = new DataflowVariable()
final def b = new DataflowVariable()

final def actor = Actors.actor {

    delegate.metaClass.onTimeout = {
        println 'Deadlock detected'
        terminate()
    }

    react(5, TimeUnit.SECONDS) {x ->
        react(5, TimeUnit.SECONDS) {y ->
            println "Got replies: a:${x} b:${b}"
        }
    }
}

task {
    b << 20 + a.val
    actor.send b.val
}

task {
    a << 10 + b.val
    actor.send a.val
}

actor.join()
