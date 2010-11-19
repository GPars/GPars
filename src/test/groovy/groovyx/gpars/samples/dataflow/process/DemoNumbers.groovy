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

package groovyx.gpars.samples.dataflow.process

import groovyx.gpars.dataflow.DataFlowChannel
import groovyx.gpars.dataflow.DataFlowQueue
import groovyx.gpars.group.DefaultPGroup

group = new DefaultPGroup()

def numbers(DataFlowChannel out) {
    group.task {
        def a = new DataFlowQueue()
        def b = new DataFlowQueue()
        def c = new DataFlowQueue()
        group.task new Prefix(c, a, 0)
        group.task new Copy(a, b, out)
        group.task new Successor(b, c)
    }
}

final DataFlowQueue ch = new DataFlowQueue()
group.task new Print('Numbers', ch)
numbers(ch)

sleep 10000