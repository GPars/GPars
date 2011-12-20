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

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.NonDaemonPGroup

/**
 * Demonstrates that multiple consumers reading off a shared DataflowQueues will load-balance the messages, so that each message
 * is read by exactly one consumer.
 *
 * @author Vaclav Pech
 */

final group = new NonDaemonPGroup()
final queue = new DataflowQueue()

final t1 = group.task {
    (1..10).each {
        println 'First task: ' + queue.val
    }
}

final t2 = group.task {
    (1..10).each {
        println 'Second task: ' + queue.val
    }
}

group.task {
    (1..20).each {queue << it}
}

[t1, t2]*.join()
group.shutdown()