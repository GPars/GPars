//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.samples

import org.gparallelizer.Asynchronizer

/**
 * Demonstrates asynchronous collection processing using Executor services through the Asynchronizer class.
 */

def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

Asynchronizer.withAsynchronizer {
    println list.collectAsync {it * 2 }

    list.iterator().eachAsync {
        println it
    }
    
    if (list.allAsync{it < 10 }) println 'The list contains only small numbers.'

    final String text = 'want to be big'
    println((text.collectAsync {it.toUpperCase()}).join())

    def animals = ['dog', 'ant', 'cat', 'whale']
    println (animals.anyAsync {it ==~ /ant/} ? 'Found an ant' : 'No ants found')
    println (animals.allAsync {it.contains('a')} ? 'All animals contain a' : 'Some animals can live without an a')
}
