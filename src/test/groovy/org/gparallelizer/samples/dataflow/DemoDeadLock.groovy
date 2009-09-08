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

package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start

/**
 * Demonstrates that deadlocks are deterministic in dataflow concurrency model. The deadlock appears reliably every time
 * the sample is run.
 * Also shows a way to exchange messages among threads and set and handle timeouts.
 */

final def a = new DataFlowVariable()
final def b = new DataFlowVariable()

final def actor = start {

    delegate.metaClass.onTimeout = {
        println 'Deadlock detected'
        System.exit 0
    }

    react(5.seconds) {x, y ->
        println "Got replies: a:${x} b:${b}"
    }
}

start {
    b << 20 + a.val
    actor.send b.val
}

start {
    a << 10 + b.val
    actor.send a.val
}

