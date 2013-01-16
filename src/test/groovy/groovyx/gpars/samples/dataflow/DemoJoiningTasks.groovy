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
 * Demonstrates composing tasks
 * When creating a task, a promise is returned, through which the task communicates back its return value and which can also be used to join the task.
 *
 * @author Vaclav Pech
 */

task {
    final Promise t1 = task {
        println 'First sub-task running.'
    }
    final Promise t2 = task {
        println 'Second sub-task running'
    }
    [t1, t2]*.join()
    println 'Both sub-tasks finished now'
}.join()

def a, b, c

task {
    final Promise t1 = task {
        a = Math.random()
    }
    final Promise t2 = task {
        b = Math.random()
    }

    [t1, t2]*.join()
    println 'Both sub-tasks finished now, calculating the sum...'
    c = (a + b) / 2
    println "The result is: $c"
    println "The result using tasks directly is: ${(t1.val + t2.val) / 2}"
}.join()

