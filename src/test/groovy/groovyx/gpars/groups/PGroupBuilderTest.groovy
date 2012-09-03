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

package groovyx.gpars.groups

import groovyx.gpars.group.PGroup
import groovyx.gpars.group.PGroupBuilder
import groovyx.gpars.scheduler.DefaultPool
import groovyx.gpars.scheduler.Pool
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool

class PGroupBuilderTest extends GroovyTestCase {
    public void testCreationFromPool() {
        final ExecutorService service = Executors.newFixedThreadPool(2)
        final pool = new DefaultPool(service)
        PGroup group = PGroupBuilder.createFromPool(pool)
        assert pool == group.threadPool
        assert group.threadPool instanceof Pool
        group.threadPool.execute({->})
        group.shutdown()
    }

    public void testCreationFromForkJoinPool() {
        final pool = new ForkJoinPool()
        PGroup group = PGroupBuilder.createFromPool(pool)
        assert pool == group.threadPool.forkJoinPool
        assert group.threadPool instanceof Pool
        group.threadPool.execute({->})
        group.shutdown()
    }

    public void testCreationFromExecutorsPool() {
        final pool = Executors.newFixedThreadPool(2)
        PGroup group = PGroupBuilder.createFromPool(pool)
        assert pool == group.threadPool.executorService
        assert group.threadPool instanceof Pool
        group.threadPool.execute({->})
        group.shutdown()
    }
}
