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
 * Demonstrates three threads calculating a sum of numbers within a given interval using shared dataflow variables.
 *
 */
List<Integer> ints(int n, int max) {
    if (n == max) return []
    else return [n, * ints(n + 1, max)]
}

List<Integer> sum(int s, List<Integer> stream) {
    switch (stream.size()) {
        case 0: return [s]
        default:
            return [s, * sum(stream[0] + s, stream.size() > 1 ? stream[1..-1] : [])]
    }
}

def x = new DataflowVariable<List<Integer>>()
def y = new DataflowVariable<List<Integer>>()

task { x << ints(0, 500) }
task { y << sum(0, x.val) }
task { println("List of sums: " + y.val); System.exit(0) }.join()
