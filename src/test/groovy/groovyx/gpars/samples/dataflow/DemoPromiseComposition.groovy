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

import groovyx.gpars.dataflow.DataflowVariable
import static groovyx.gpars.dataflow.Dataflow.task

/**
 * Promises (aka DataflowVariables) can be composed using the << operator
 *
 * @author Vaclav Pech
 */

DataflowVariable promise1 = new DataflowVariable()
DataflowVariable promise2 = new DataflowVariable()
DataflowVariable promise3 = new DataflowVariable()
DataflowVariable promise4 = new DataflowVariable()

promise1 << (promise2 << promise3)
promise4 << promise2

promise3 << task { 10 }

println promise1.val
println promise2.val
println promise3.val
println promise4.val
