// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

import groovyx.gpars.GParsExecutorsPoolEnhancer

/**
 * Demonstrates parallel collection processing using ParallelArrays through the ParallelEnhancer class.
 */

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

GParsExecutorsPoolEnhancer.enhanceInstance(list)

println list.collectParallel {it * 2 }

final Iterator iterator = list.iterator()
GParsExecutorsPoolEnhancer.enhanceInstance iterator

iterator.eachParallel {
    println it
}

final String text = 'want to be very big'
GParsExecutorsPoolEnhancer.enhanceInstance text
println((text.collectParallel {it.toUpperCase()}).join())

def animals = ['dog', 'ant', 'cat', 'whale']
GParsExecutorsPoolEnhancer.enhanceInstance animals
println(animals.anyParallel {it ==~ /ant/} ? 'Found an ant' : 'No ants found')
println(animals.everyParallel {it.contains('a')} ? 'All animals contain a' : 'Some animals can live without an a')
