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

package groovyx.gpars.samples.group

import groovyx.gpars.GParsExecutorsPool
import groovyx.gpars.GParsPool
import groovyx.gpars.group.PGroup
import groovyx.gpars.group.PGroupBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

/**
 * Demonstrates how to use existing thread pool to build an instance of PGroup and then retrieve the pool back from the group.
 * The ability to reuse the same thread pool by instances of parallel groups may come in handy when building applications
 * combining multiple GPars concepts, e.g. dataflow tasks with parallel collections.
 */

GParsPool.withPool {ForkJoinPool pool ->
    [1, 2, 3, 4, 5].eachParallel {println it}

    final PGroup group = PGroupBuilder.createFromPool(pool)
    group.task {
        println 'Printing this asynchronously'
    }.join()

    group.threadPool.execute {println 'Printing this asynchronously as well'}
    group.threadPool.forkJoinPool.execute {println 'Printing this asynchronously, too'}
}


GParsExecutorsPool.withPool {ExecutorService pool ->
    [1, 2, 3, 4, 5].eachParallel {println it}

    final PGroup group = PGroupBuilder.createFromPool(pool)
    group.task {
        println 'Printing this asynchronously'
    }.join()

    group.threadPool.execute {println 'Printing this asynchronously as well'}
    group.threadPool.executorService.execute {println 'Printing this asynchronously, too'}
}
