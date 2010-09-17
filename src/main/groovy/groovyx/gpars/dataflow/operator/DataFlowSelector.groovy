// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.group.PGroup
import java.util.concurrent.Semaphore

/**
 * Dataflow selectors and operators (processors) form the basic units in dataflow networks. They are typically combined into oriented graphs that transform data.
 * They accept a set of input and output dataflow channels so that once values are available to be consumed in all
 * the input channels the processor's body is triggered on the values, potentially generating values for the output channels.
 * The output channels at the same time are suitable to be used as input channels by some other dataflow processors.
 * The channels allow processors to communicate.
 *
 * Dataflow selectors and operators enable creation of highly concurrent applications yet the abstraction hides the low-level concurrency primitives
 * and exposes much friendlier API.
 * Since selectors and operators internally leverage the actor implementation, they reuse a pool of threads and so the actual number of threads
 * used by the calculation can be kept much lower than the actual number of processors used in the network.
 *
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */
public class DataFlowSelector extends DataFlowProcessor {

    /**
     * Creates a selector
     * After creation the selector needs to be started using the start() method.
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The selector's body to run each time all inputs have a value to read
     */
    protected def DataFlowSelector(final PGroup group, final Map channels, final Closure code) {
        super(group, channels, code)
        if (shouldBeMultiThreaded(channels)) {
            if (channels.maxForks < 1) throw new IllegalArgumentException("The maxForks argument must be a positive value. ${channels.maxForks} was provided.")
            this.actor = new ForkingDataFlowOperatorActor(this, group, channels.outputs?.asImmutable(), channels.inputs.asImmutable(), code.clone(), channels.maxForks)
        } else {
            this.actor = new DataFlowOperatorActor(this, group, channels.outputs?.asImmutable(), channels.inputs.asImmutable(), code.clone())
        }
    }

    /**
     * Is invoked in case the actor throws an exception.
     */
    protected void reportError(Throwable e) {
        System.err.println "The dataflow selector experienced an exception and is about to terminate. $e"
        stop()
    }
}

/**
 * An selector's internal actor. Repeatedly polls inputs and once they're all available it performs the selector's body.
 *
 * Iteratively waits for enough values from inputs.
 * Once all required inputs are available (received as messages), the selector's body is run.
 */
private class DataFlowSelectorActor extends DynamicDispatchActor {
    private final List inputs
    protected final List outputs
    protected final Closure code
    private final def owningOperator
    private Map values = [:]

    def DataFlowSelectorActor(owningOperator, group, outputs, inputs, code) {
        super(null)
        parallelGroup = group

        this.owningOperator = owningOperator
        this.outputs = outputs
        this.inputs = inputs
        this.code = code
    }

    final void onMessage(def message) {
        values[message.attachment] = message.result
        assert values.size() <= inputs.size()
        if (values.size() == inputs.size()) {
            def results = values.sort {it.key}.values() as List
            startTask(results)
            values = [:]
            queryInputs()
        }
    }

    final void afterStart() {
        queryInputs()
    }

    private def queryInputs() {
        return inputs.eachWithIndex {input, index -> input.getValAsync(index, this)}
    }

    def startTask(results) {
        try {
            code.call(* results)
        } catch (Throwable e) {
            reportException(e)
        }
    }

    final reportException(Throwable e) {
        owningOperator.reportError(e)
    }
}

/**
 * An selector's internal actor. Repeatedly polls inputs and once they're all available it performs the selector's body.
 * The selector's body is executed in as a separate task, allowing multiple copies of the body to be run concurrently.
 * The maxForks property guards the maximum number or concurrently run copies.
 */
private final class ForkingDataFlowSelectorActor extends DataFlowSelectorActor {
    private final Semaphore semaphore
    private final def threadPool

    def ForkingDataFlowSelectorActor(owningOperator, group, outputs, inputs, code, maxForks) {
        super(owningOperator, group, outputs, inputs, code)
        this.semaphore = new Semaphore(maxForks)
        this.threadPool = group.threadPool
    }

    def startTask(results) {
        semaphore.acquire()
        threadPool.execute {
            try {
                code.call(* results)
            } catch (Throwable e) {
                reportException(e)
            } finally {
                semaphore.release()
            }
        }
    }
}

