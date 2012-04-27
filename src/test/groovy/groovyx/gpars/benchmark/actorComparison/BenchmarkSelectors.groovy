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

package groovyx.gpars.benchmark.actorComparison

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.FJPool

final def concurrencyLevel = 8
group = new DefaultPGroup(new FJPool(concurrencyLevel))

final DataflowQueue queue = new DataflowQueue()

(1..2000000).each {
    queue << it
}
queue << -1

final def t1 = System.currentTimeMillis()

long sum = 0
def op = group.selector([queue], []) {
    if (it == -1) {
        println sum
        terminate()
    } else {
        sum += it
    }
}

op.join()
group.shutdown()
final def t2 = System.currentTimeMillis()
println(t2 - t1)
