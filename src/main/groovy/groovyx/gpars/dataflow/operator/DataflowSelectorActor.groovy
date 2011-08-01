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

/**
 * An selector's internal actor. Repeatedly polls inputs and once they're all available it performs the selector's body.
 *
 * Iteratively waits for values on the inputs.
 * Once all a value is available (received as a message), the selector's body is run.
 *
 * @author Vaclav Pech
 */
private class DataflowSelectorActor extends DataflowProcessorActor {
    protected final boolean passIndex = false

    def DataflowSelectorActor(owningOperator, group, outputs, inputs, code) {
        super(owningOperator, group, outputs, inputs, code)
        if (code.maximumNumberOfParameters == 2) {
            passIndex = true
        }
    }

    void afterStart() {
        owningProcessor.doSelect()
    }

    final void onMessage(Object message) {
        if (message instanceof StopGently) {
            stoppingGently = true
            return
        }
        final def index = message.index
        final def value = message.value
        if (checkPoison(value)) return
        startTask(index, value)
        if (stoppingGently) {
            stop()
        }
        if (!hasBeenStopped()) owningProcessor.doSelect()
    }

    def startTask(index, result) {
        try {
            if (passIndex) {
                code.call(result, index)
            } else {
                code.call(result)
            }
        } catch (Throwable e) {
            reportException(e)
        }
    }
}
