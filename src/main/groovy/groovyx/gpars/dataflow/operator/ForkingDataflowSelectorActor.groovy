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

package groovyx.gpars.dataflow.operator

import java.util.concurrent.Semaphore

/**
 * An selector's internal actor. Repeatedly polls inputs and once they're all available it performs the selector's body.
 * The selector's body is executed in as a separate task, allowing multiple copies of the body to be run concurrently.
 * The maxForks property guards the maximum number or concurrently run copies.
 *
 * @author Vaclav Pech
 */
private final class ForkingDataflowSelectorActor extends DataflowSelectorActor {
    private final Semaphore semaphore
    private final def threadPool

    def ForkingDataflowSelectorActor(owningOperator, group, outputs, inputs, code, maxForks) {
        super(owningOperator, group, outputs, inputs, code)
        this.semaphore = new Semaphore(maxForks)
        this.threadPool = group.threadPool
    }

    @Override
    def startTask(index, result) {
        semaphore.acquire()
        threadPool.execute {
            try {
                super.startTask(index, result)
            } finally {
                semaphore.release()
            }
        }
    }
}
