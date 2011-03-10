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

import static groovyx.gpars.GParsPool.speculate
import static groovyx.gpars.GParsPool.withPool

/**
 * Demonstrates speculations - an ability to run in parallel several calculations of the same value with varying time demands,
 * harvesting the first obtained result and cancelling the slower calculations.
 *
 * @author Vaclav Pech
 * Date: Aug 23rd 2010
 */
def alternative1 = {
    Thread.sleep 10000
    println 'Never reached'
    10
}

def alternative2 = {
    Thread.sleep 1000
    20
}

def alternative3 = {
    Thread.sleep 10
    throw new RuntimeException('test')
}

def alternative4 = {
    Thread.sleep 1000
    40
}

withPool(5) {
    println speculate([alternative1, alternative2, alternative3, alternative4])
}
