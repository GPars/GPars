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

package groovyx.gpars.samples.collections

import groovyx.gpars.ParallelEnhancer

/**
 * Demonstrates parallel collection processing using ParallelArrays through the ParallelEnhancer class.
 */

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

ParallelEnhancer.enhanceInstance(list)
list.makeConcurrent()

println list.collect { it * 2 }

final Iterator iterator = list.iterator()
ParallelEnhancer.enhanceInstance iterator

iterator.each {
    println it
}

println "Minimum: ${list.min()}"
println "Minimum: ${list.min { a, b -> a - b }}"
println "Maximum: ${list.max()}"
println "Maximum: ${list.max { a, b -> a - b }}"
println "Sum: ${list.sum()}"
println "Product: ${list.inject { a, b -> a * b }}"

final String text = 'want to be so big'
ParallelEnhancer.enhanceInstance text
text.makeConcurrent()
println((text.collectParallel { it.toUpperCase() }).join())

def animals = ['dog', 'ant', 'cat', 'whale']
ParallelEnhancer.enhanceInstance animals
animals.makeConcurrent()
println(animals.any { it ==~ /ant/ } ? 'Found an ant' : 'No ants found')
println(animals.every { it.contains('a') } ? 'All animals contain a' : 'Some animals can live without an a')

//Using transparent parallelism here with method chaining. The iterative methods collect() and groupBy()
// here use parallel implementation under the covers
println(animals.makeConcurrent().collect { it.toUpperCase() }.groupBy { it.contains 'A' })

/**
 * A function implemented using standard sequential collect() and findAll() methods.
 */
def selectImportantNames(names) {
    names.collect { it.toUpperCase() }.findAll { it.size() > 4 }
}

def names = ['Joe', 'Alice', 'Dave', 'Jason']
ParallelEnhancer.enhanceInstance(names)
//The selectImportantNames() will process the name collections concurrently
assert ['ALICE', 'JASON'] == selectImportantNames(names.makeConcurrent())
