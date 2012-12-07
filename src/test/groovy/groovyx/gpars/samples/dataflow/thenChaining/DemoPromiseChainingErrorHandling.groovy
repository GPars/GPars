// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.samples.dataflow.thenChaining

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

[10, 1, 0].each { num ->
    Promise<Integer> initial = new DataflowVariable<Integer>()
    Promise<String> result = initial.then { it * 2 } then { 100 / it }
            .then { println "Logging the value $it as it passes by"; return it }      //Since no error handler is defined, exceptions will be ignored
    //and silently re-thrown to the next handler in the chain
            .then({ "The result for $num is $it" }, { "Error detected for $num: $it" }) //Here the exception is caught
    initial << num
    println result.get()
}