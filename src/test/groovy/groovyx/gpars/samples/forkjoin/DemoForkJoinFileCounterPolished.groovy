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

package groovyx.gpars.samples.forkjoin

import groovy.io.FileType
import static groovyx.gpars.GParsPool.runForkJoin
import static groovyx.gpars.GParsPool.withPool

/**
 * Shows use of the ForkJoin mechanics to count files recursively in a directory.
 *
 * Author: Vaclav Pech
 */

/**
 Fork/Join operations can be safely run with small number of threads thanks to using the TaskBarrier class to synchronize the threads.
 Although the algorithm creates as many tasks as there are sub-directories and tasks wait for the sub-directory tasks to complete,
 as few as one thread is enough to keep the computation going.
 */

withPool(1) {pool ->  //feel free to experiment with the number of fork/join threads in the pool
    println """Number of files: ${
        runForkJoin(new File("./src")) {File file ->
            long count = 0
            file.eachDir {forkOffChild(it)}
            file.eachFile FileType.FILES, {
                count++
            }
            return count + (childrenResults.sum(0))
            //use results of children tasks to calculate and store own result
        }
    }"""
}
