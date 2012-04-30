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

package groovyx.gpars.benchmark.akka

import groovyx.gpars.actor.Actor
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.DefaultPool

/**
 * https://github.com/jboner/akka-bench
 *
 * @author Jiri Mares, Vaclav Pech
 */
final class PipelineBenchmark {
    Actor writer, indexer, downloader
    def actors = []

    def create() {
        def concurrencyLevel = 4
        new DefaultPGroup(new DefaultPool(false, concurrencyLevel))
    }

    def prepare(group, boolean start) {
        actors = [writer, indexer, downloader]
        if (start) {
            actors*.parallelGroup = group
            actors*.start()
        }
    }

    def benchmark() {
        final def t1 = System.currentTimeMillis()

        for (int i = 0; i < 1000000; i++) {
            downloader << ('Requested ' + i)
        }

        downloader << StopMessage.instance
        actors*.join()
        final def t2 = System.currentTimeMillis()

        t2 - t1
    }

    String run() {
        downloader.follower = indexer
        indexer.follower = writer

        def group = create()
        prepare(group, true)
        final result = benchmark()
        shutdown(group)
        result
    }

    void warmup() {
        run()
    }

    void shutdown(group) {
        group.shutdown()
    }
}