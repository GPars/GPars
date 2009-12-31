//  GPars (formerly GParallelizer)
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

package groovyx.gpars.samples.forkjoin

import groovyx.gpars.AbstractForkJoinWorker
import static groovyx.gpars.Parallelizer.doParallel
import static groovyx.gpars.Parallelizer.orchestrate

/**
 * Shows use of the ForkJoin mechanics to count files recursively in a directory.
 *
 * Author: Vaclav Pech
 * Date: Nov 1, 2008
 */

public final class FileCounter extends AbstractForkJoinWorker<Long> {
    private final File file;

    def FileCounter(final File file) {
        this.file = file
    }

    protected void compute() {
        long count = 0;
        file.eachFile {
            if (it.isDirectory()) {
                println "Forking a thread for $it"
                forkOffChild(new FileCounter(it))           //fork a child task
            } else {
                count++
            }
        }
        setResult(count + ((childrenResults)?.sum() ?: 0))  //use results of children tasks to calculate and store own result
    }
}

/**
 Fork/Join operations can be safely run with small number of threads thanks to using the TaskBarrier class to synchronize the threads.
 Although the algorithm creates as many tasks as there are sub-directories and tasks wait for the sub-directory tasks to complete,
 as few as one thread is enough to keep the computation going.
 */

doParallel(1) {pool ->  //feel free to experiment with the number of fork/join threads in the pool
    println "Number of files: ${orchestrate(new FileCounter(new File("..")))}"
}
