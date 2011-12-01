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

package groovyx.gpars.samples.dataflow.operators.chaining

import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.SyncDataflowBroadcast

/**
 * The chainWith() method available on all channels allows you to build pipe-lines off the original channel.
 * The type of the channel gets preserved across the whole chain.
 *
 * @author Vaclav Pech
 */

final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()

final DataflowReadChannel subscription1 = broadcast.createReadChannel()
def result1 = subscription1.chainWith {it * 2}.chainWith {it + 1} chainWith {it * 100}

final DataflowReadChannel subscription2 = broadcast.createReadChannel()
def result2 = subscription2.chainWith {it * 3}.chainWith {it + 1} chainWith {it * 1000}

Thread.start {
    5.times {
        println 'Subscription1: ' + result1.val
    }
}
Thread.start {
    5.times {
        println 'Subscription2: ' + result2.val
    }
}
broadcast << 1
broadcast << 2
broadcast << 3
broadcast << 4
broadcast << 5

sleep 1000