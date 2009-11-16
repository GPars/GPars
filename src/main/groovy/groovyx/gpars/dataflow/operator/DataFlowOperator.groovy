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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.ActorGroup
import groovyx.gpars.actor.PooledActorGroup

/**
 * Dataflow operators form the basic units in dataflow networks. Operators are typically combined into oriented graphs that transform data.
 * They accept a set of input and output dataflow channels so that once values are available to be consumed in all
 * the input channels the operator's body is triggered on the values, potentially generating values for the output channels.
 * The output channels at the same time are suitable to be used as input channels by some other dataflow operators.
 * The channels allow operators to communicate.
 *
 * Dataflow operators enable creation of highly concurrent applications yet the abstraction hides the low-level concurrency primitives
 * and exposes much friendlier API.
 * Since operators internaly leverage the actor implementation, they reuse a pool of threads and so the actual number of threads
 * used by the calculation can be kept much lower than the actual number of operators used in the network.
 *
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */
public final class DataFlowOperator {

    private static final dfOperatorActorGroup = new PooledActorGroup()

    /**
     * Creates an operator using the default operator actor group
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public static DataFlowOperator operator(final Map channels, final Closure code) {
        return new DataFlowOperator(channels, code).start(dfOperatorActorGroup)
    }

    /**
     * Creates an operator using the specified operator actor group
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param group The operator actor group to use with the operator
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public static DataFlowOperator operator(final Map channels, final ActorGroup group, final Closure code) {
        return new DataFlowOperator(channels, code).start(group)
    }

    private final List inputs
    private final List outputs
    private final Closure code
    private final Actor actor

    /**
     * Creates an operator
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The operator's body to run each time all inputs have a value to read
     */
    private def DataFlowOperator(final Map channels, final Closure code) {
        this.outputs = channels.outputs.asImmutable()
        this.code = code.clone()
        this.code.delegate = this
        this.inputs = channels.inputs.asImmutable()

        final int parameters = code.maximumNumberOfParameters
        if (parameters != this.inputs.size())
            throw new IllegalArgumentException("The operator's body accepts $parameters parameters while it is given ${this.inputs.size()} input streams. The numbers must match.")
    }

    /**
     * Starts an operator using the specified operator actor group
     * @param group The operator actor group to use with the operator
     */
    private DataFlowOperator start(ActorGroup group) {
        actor = group.actor {
            loop {
                inputs.eachWithIndex {input, index -> input.getValAsync(index, actor)}
                def values = [:]
                handleValueMessage(values, inputs.size())
            }
        }
        actor.start()
        return this
    }

    private void handleValueMessage(Map values, count) {
        if (values.size() < count) {
            actor.react {
                values[it.attachment] = it.result
                handleValueMessage(values, count)
            }
        } else {
            def results = values.sort {it.key}.values() as List
            code.call(* results)
        }
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
    Map bindOutput(final int idx, final value) {
        outputs[idx] << value
    }

    /**
     * Used by the operator's body to send a value to the first / only output channel
     */
    Map bindOutput(final value) { bindOutput 0, value }

    /**
     * The operator's output channel of the given index
     */
    public getOutputs(int idx) { outputs[idx] }

    /**
     * The operator's first / only output channel
     */
    public getOutput() { outputs[0] }
}