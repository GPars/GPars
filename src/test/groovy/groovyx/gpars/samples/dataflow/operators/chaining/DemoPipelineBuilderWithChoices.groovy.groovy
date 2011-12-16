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
import groovyx.gpars.dataflow.operator.Pipeline
import groovyx.gpars.scheduler.DefaultPool
import groovyx.gpars.scheduler.Pool

/**
 * Demonstrates a more involved use of the pipeline builder (aka the Pipeline class) to build a dataflow pipeline.
 *
 * @author Vaclav Pech
 */

final DataflowQueue queue = new DataflowQueue()
final DataflowQueue nonNegative = new DataflowQueue()
final DataflowQueue negative = new DataflowQueue()
final DataflowQueue converted = new DataflowQueue()
final Pool pool = new DefaultPool(false, 2)

final DataflowQueue logChannel = new DataflowQueue()
logChannel | {println 'LOG: ' + it}

final negate = {-it}

final Pipeline pipeline = new Pipeline(pool, queue)

pipeline.binaryChoice(nonNegative, negative) {it >= 0}

negative | negate | converted

queue << 1
queue << -2
queue << 3

assert 1 == nonNegative.val
assert 3 == nonNegative.val
assert 2 == converted.val

pool.shutdown()