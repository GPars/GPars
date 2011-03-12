// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.collections

import groovyx.gpars.GParsPool

/**
 * This demo shows that maps can be processed in parallel just like other collections.
 * Also, the map-specific, two-argument closure can be passed to the relevant parallel methods.
 */
def map = [a: 1, b: 2, c: 3, d: 4, e: 5]
GParsPool.withPool {
    map.eachParallel {item -> println "$item.key : $item.value"}
    map.eachParallel {k, v -> println "$k : $v"}
    println map.collectParallel {item -> "[$item.key : $item.value]"}
    println map.collectParallel {k, v -> "[$k : $v]"}

    println map.makeConcurrent().collect {k, v -> "[$k : $v]"}
}