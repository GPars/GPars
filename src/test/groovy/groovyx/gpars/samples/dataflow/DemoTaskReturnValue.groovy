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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.Promise

import static groovyx.gpars.dataflow.Dataflow.task

/**
 * Demonstrates composing tasks and obtaining their return values
 * When creating a task, a promise is returned, through which the task communicates back its return value and which can also be used to join the task.
 *
 * @author Vaclav Pech
 */

task {
    final Promise t1 = task {
        return 10
    }
    final Promise t2 = task {
        return 20
    }
    def results = [t1, t2]*.val
    println 'Both sub-tasks finished and returned values: ' + results
}

//Asynchronous result retrieval using whenBound() (>>)

def task = task {
    println 'The task is running and calculating the return value'
    30
}
task >> { value -> println "The task finished and returned $value" }
