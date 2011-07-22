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

package groovyx.gpars.samples.dataflow.synchronous

import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.Select
import groovyx.gpars.dataflow.SyncDataflowBroadcast
import groovyx.gpars.dataflow.SyncDataflowQueue
import static groovyx.gpars.dataflow.Dataflow.select

/**
 * Shows the use of synchronous channels with Selects
 */

final DataflowReadChannel queue = new SyncDataflowQueue()
final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
final DataflowReadChannel subscription = broadcast.createReadChannel()

final Select select = select([queue, subscription])
Thread.start {
    queue << 1
    queue << 2
    broadcast << 3
    broadcast << 4
    queue << 5
}
println select().value
println select().value
println select().value
println select().value
println select().value
