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

import groovyx.gpars.actor.AbstractPooledActor
import groovyx.gpars.group.PGroup
import java.util.concurrent.Semaphore

/**
 * Dataflow operators form the basic units in dataflow networks. Operators are typically combined into oriented graphs that transform data.
 * They accept a set of input and output dataflow channels so that once values are available to be consumed in all
 * the input channels the operator's body is triggered on the values, potentially generating values for the output channels.
 * The output channels at the same time are suitable to be used as input channels by some other dataflow operators.
 * The channels allow operators to communicate.
 *
 * Dataflow operators enable creation of highly concurrent applications yet the abstraction hides the low-level concurrency primitives
 * and exposes much friendlier API.
 * Since operators internally leverage the actor implementation, they reuse a pool of threads and so the actual number of threads
 * used by the calculation can be kept much lower than the actual number of operators used in the network.
 *
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */
public final class DataFlowOperator {

    /**
     * The internal actor performing on behalf of the operator
     */
    private final DataFlowOperatorActor actor

    /**
     * Creates an operator
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The operator's body to run each time all inputs have a value to read
     */
    private def DataFlowOperator(final Map channels, final Closure code) {
        final int parameters = code.maximumNumberOfParameters
        if (!channels || (channels.inputs == null) || (parameters != channels.inputs.size()))
            throw new IllegalArgumentException("The operator's body accepts $parameters parameters while it is given ${channels?.inputs?.size()} input streams. The numbers must match.")

        code.delegate = this
        if (channels.maxForks != null && channels.maxForks != 1) {
            if (channels.maxForks < 1) throw new IllegalArgumentException("The maxForks argument must be a positive value. ${channels.maxForks} was provided.")
            this.actor = new ForkingDataFlowOperatorActor(this, channels.outputs?.asImmutable(), channels.inputs.asImmutable(), code.clone(), channels.maxForks)
        } else {
            this.actor = new DataFlowOperatorActor(this, channels.outputs?.asImmutable(), channels.inputs.asImmutable(), code.clone())
        }
    }

    /**
     * Starts an operator using the specified operator actor group
     * @param group The operator actor group to use with the operator
     */
    DataFlowOperator start(PGroup group) {
        actor.parallelGroup = group
        actor.start()
        return this
    }

    /**
     * Stops the operator
     */
    public void stop() { actor.stop() }

    /**
     * Joins the operator waiting for it to finish
     */
    public void join() { actor.join() }

    /**
     * Used by the operator's body to send a value to the given output channel
     */
    void bindOutput(final int idx, final value) {
        actor.outputs[idx] << value
    }

    /**
     * Used by the operator's body to send a value to the first / only output channel
     */
    void bindOutput(final value) { bindOutput 0, value }

    /**
     * The operator's output channel of the given index
     */
    public getOutputs(int idx) { actor.outputs[idx] }

    /**
     * The operator's output channel of the given index
     */
    public getOutputs() { actor.outputs }

    /**
     * The operator's first / only output channel
     */
    public getOutput() { actor.outputs[0] }

    /**
     * Is invoked in case the actor throws an exception.
     */
    protected void reportError(Throwable e) {
        System.err.println "The dataflow operator experienced an exception and is about to terminate. $e"
        stop()
    }
}

/**
 * An operator's internal actor. Repeatedly polls inputs and once they're all available it performs the operator's body.
 */
private class DataFlowOperatorActor extends AbstractPooledActor {
    final List inputs
    final List outputs
    final Closure code
    final def owningOperator

    def DataFlowOperatorActor(owningOperator, outputs, inputs, code) {
        this.owningOperator = owningOperator
        this.outputs = outputs
        this.inputs = inputs
        this.code = code
    }

    protected final void act() {
        loop {
            inputs.eachWithIndex {input, index -> input.getValAsync(index, this)}
            def values = [:]
            handleValueMessage(values, inputs.size())
        }
    }

    /**
     * Calls itself recursively within a react() call, if more input values are still needed.
     * Once all required inputs are available (received as messages), the operator's body is run.
     */
    final void handleValueMessage(Map values, count) {
        if (values.size() < count) {
            react {
                values[it.attachment] = it.result
                handleValueMessage(values, count)
            }
        } else {
            def results = values.sort {it.key}.values() as List
            startTask(results)
        }
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
 * An operator's internal actor. Repeatedly polls inputs and once they're all available it performs the operator's body.
 * The operator's body is executed in as a separate task, allowing multiple copies of the body to be run concurrently.
 * The maxForks property guards the maximum number or concurrently run copies.
 */
private final class ForkingDataFlowOperatorActor extends DataFlowOperatorActor {
    final Semaphore semaphore

    def ForkingDataFlowOperatorActor(owningOperator, outputs, inputs, code, maxForks) {
        super(owningOperator, outputs, inputs, code)
        this.semaphore = new Semaphore(5)
    }

    def startTask(results) {
        semaphore.acquire()
        parallelGroup.threadPool.execute {
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
