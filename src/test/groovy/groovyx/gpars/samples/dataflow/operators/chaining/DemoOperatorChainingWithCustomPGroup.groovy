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

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.NonDaemonPGroup

/**
 * To specify the thread pool or a PGroup instance to use by the operators in the pipe-line, just pass them as parameters to the chainWith() method.
 *
 * @author Vaclav Pech
 */

final NonDaemonPGroup group = new NonDaemonPGroup(2)

final DataflowQueue queue = new DataflowQueue()
queue.chainWith(group) {it * 2}.chainWith(group) {it + 1}.chainWith(group) {println it}

queue << 1
queue << 2
queue << 3
queue << 4
queue << 5

sleep 1000
group.shutdown()