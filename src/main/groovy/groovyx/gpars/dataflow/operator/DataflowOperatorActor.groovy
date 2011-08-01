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

package groovyx.gpars.dataflow.operator;


import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.PGroup

/**
 * An operator's internal actor. Repeatedly polls inputs and once they're all available it performs the operator's body.
 *
 * Iteratively waits for enough values from inputs.
 * Once all required inputs are available (received as messages), the operator's body is run.
 *
 * @author Vaclav Pech
 */
class DataflowOperatorActor extends DataflowProcessorActor {
    Map values = new HashMap();

    def DataflowOperatorActor(DataflowOperator owningOperator, PGroup group, List outputs, List inputs, Closure code) {
        super(owningOperator, group, outputs, inputs, code)
    }

    final void afterStart() {
        queryInputs(true)
    }

    private final def queryInputs(final boolean initialRun) {
        return inputs.eachWithIndex {input, index ->
            if (initialRun || !(input instanceof DataflowVariable)) {
                input.getValAsync(index, this)
            } else {
                values[index] = input.val
            }
        }
    }

    @Override
    final void onMessage(Object message) {
        if (message instanceof StopGently) {
            stoppingGently = true
            return
        }
        if (checkPoison(message.result)) return
        values[message.attachment] = message.result
        if (values.size() > inputs.size()) throw new IllegalStateException("The DataflowOperatorActor is in an inconsistent state. values.size()=" + values.size() + ", inputs.size()=" + inputs.size())
        if (values.size() == inputs.size()) {
            def results = values.sort {it.key}.values() as List
            startTask(results)
            values = [:]
            if (stoppingGently) {
                stop()
            }
            if (!hasBeenStopped()) queryInputs(false)
        }
    }

    void startTask(results) {
        try {
            code.call(* results)
        } catch (Throwable e) {
            reportException(e)
        }
    }
}
