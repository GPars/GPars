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

package groovyx.gpars

import jsr166y.forkjoin.RecursiveTask
import groovyx.gpars.dataflow.DataFlowVariable

/**
 * Represents tasks submitted to the thread pool as parts of asynchronous function processing
 *
 * @author Vaclav Pech
 */
final class AsyncFunTask extends RecursiveTask {
    private DataFlowVariable result
    private Closure original
    private def soFarArgs

    AsyncFunTask(final DataFlowVariable result, final Closure original, final soFarArgs) {
        this.result = result
        this.original = original
        this.soFarArgs = soFarArgs
    }

    @Override protected Object compute() {
        try {
            result << original(* soFarArgs)
        } catch (all) {
            result << all
        }
    }

}
