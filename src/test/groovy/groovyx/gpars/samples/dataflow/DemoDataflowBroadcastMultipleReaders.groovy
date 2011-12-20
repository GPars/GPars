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

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.group.NonDaemonPGroup

/**
 * Demonstrates that multiple consumers reading off a shared DataflowBroadcast will each receive all the messages.
 *
 * @author Vaclav Pech
 */

final group = new NonDaemonPGroup()
final broadcast = new DataflowBroadcast()

def subscription1 = broadcast.createReadChannel()
final t1 = group.task {
    (1..20).each {
        println 'First task: ' + subscription1.val
    }
}

def subscription2 = broadcast.createReadChannel()
final t2 = group.task {
    (1..20).each {
        println 'Second task: ' + subscription2.val
    }
}

group.task {
    (1..20).each {broadcast << it}
}

[t1, t2]*.join()
group.shutdown()
