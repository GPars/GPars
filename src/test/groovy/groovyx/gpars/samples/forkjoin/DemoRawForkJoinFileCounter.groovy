// GPars - Groovy Parallel Systems
//
// Copyright © 2008--2011  The original author or authors
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

package groovyx.gpars.samples.forkjoin

import groovyx.gpars.GParsPool
import java.util.concurrent.RecursiveTask

/**
 * Shows use of the ForkJoin mechanics to count files recursively in a directory.
 *
 * Author: Vaclav Pech
 * Date: Jul 16, 2008
 */

public class FJFileCounter extends RecursiveTask<Long> {
    private final File file;

    def FJFileCounter(final File file) {
        this.file = file
    }

    protected Long compute() {
        def fileCounters = []
        long count = 0
        file.eachFile {
            if (it.isDirectory()) {
                println "Forking a thread for $it"
                def childCounter = new FJFileCounter(it)
                childCounter.fork()
                fileCounters.add(0, childCounter)
            } else {
                count++
            }
        }
        count += (fileCounters*.join())?.sum() ?: 0
        return count
    }
}

/**
 Fork/Join operations can be safely run with small number of threads thanks to using the TaskBarrier class to synchronize the threads.
 Although the algorithm creates as many tasks as there are sub-directories and tasks wait for the sub-directory tasks to complete,
 as few as one thread is enough to keep the computation going.
 */
GParsPool.withPool(1) {pool ->  //feel free to experiment with the number of fork/join threads in the pool
    def result = pool.submit(new FJFileCounter(new File('./src'))).get()
    println "Number of files: ${result}"
}
