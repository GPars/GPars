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

import groovyx.gpars.Parallelizer
import groovyx.gpars.dataflow.DataFlowVariable
import jsr166y.forkjoin.RecursiveAction
import jsr166y.forkjoin.TaskBarrier

/**
 * Shows use of the ForkJoin mechanics to count files recursively in a directory.
 *
 * Author: Vaclav Pech
 * Date: Jul 16, 2008
 */

public class FJFileCounter extends RecursiveAction {
    private final File file;
    private final TaskBarrier taskBarrier;

    long count = 0;

    def FJFileCounter(final File file, final TaskBarrier taskBarrier) {
        this.file = file
        this.taskBarrier = taskBarrier
        this.taskBarrier.register()
    }

    protected void compute() {
        final TaskBarrier childTaskBarrier = new TaskBarrier(1)
        def fileCounters = []
        file.eachFile {
            if (it.isDirectory()) {
                println "Forking a thread for $it"
                def childCounter = new FJFileCounter(it, childTaskBarrier)
                childCounter.fork()
                fileCounters << childCounter
            } else {
                count++
            }
        }
        childTaskBarrier.arriveAndAwait()
        this.count += (fileCounters*.count)?.sum() ?: 0
        taskBarrier.arriveAndDeregister()
    }
}

class Orchestrator extends RecursiveAction {
    String dir
    DataFlowVariable result

    protected void compute() {
        final TaskBarrier taskBarrier = new TaskBarrier(1)
        final def counter = new FJFileCounter(new File(dir), taskBarrier)
        counter.fork()
        taskBarrier.arriveAndAwait()
        result << counter.count
    }
}

/**
 Fork/Join operations can be safely run with small number of threads thanks to using the TaskBarrier class to synchronize the threads.
 Although the algorithm creates as many tasks as there are subdirectories and tasks wait for the subdirectory tasks to complete,
 as few as one thread is enough to keep the computation going.
 */

Parallelizer.doParallel(1) {pool ->  //feel free to experiment with the number of fork/join threads in the pool
    final DataFlowVariable result = new DataFlowVariable()
    final String dir = ".."
    pool.execute(new Orchestrator(dir:dir, result:result))
    println "Number of files: ${result.val}"
}
