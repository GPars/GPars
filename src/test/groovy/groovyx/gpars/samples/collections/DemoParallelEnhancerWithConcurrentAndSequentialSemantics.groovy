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

package groovyx.gpars.samples.collections

import groovyx.gpars.ParallelEnhancer

/**
 * Demonstrates parallel collection processing using ParallelArrays through the ParallelEnhancer class.
 * Shows the makeConcurrent() and makeSequential() methods, which enable switching between concurrent and sequential semantics on collections.
 */

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

println 'Sequential: '
list.each { print it + ',' }
println()

ParallelEnhancer.enhanceInstance(list)

println 'Sequential: '
list.each { print it + ',' }
println()

list.makeConcurrent()

println 'Concurrent: '
list.each { print it + ',' }
println()

list.makeSequential()

println 'Sequential: '
list.each { print it + ',' }
println()


