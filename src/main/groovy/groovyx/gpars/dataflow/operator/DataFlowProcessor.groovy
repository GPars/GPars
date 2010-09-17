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

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.group.PGroup

/**
 * Dataflow selectors and operators (processors) form the basic units in dataflow networks. They are typically combined into oriented graphs that transform data.
 * They accept a set of input and output dataflow channels and following specific strategies they transform input values from the input channels
 * into new values written to the output channels.
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
abstract class DataFlowProcessor {

    /**
     * The internal actor performing on behalf of the processor
     */
    protected Actor actor

    /**
     * Creates a processor
     * After creation the processor needs to be started using the start() method.
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The processor's body to run each time all inputs have a value to read
     */
    protected def DataFlowProcessor(final PGroup group, final Map channels, final Closure code) {
        if (channels?.inputs?.size() == 0) throw new IllegalArgumentException("The processor body must take some inputs. The provided list of input channels is empty.")
        code.delegate = this
    }

    protected boolean shouldBeMultiThreaded(Map channels) {
        return channels.maxForks != null && channels.maxForks != 1
    }

    /**
     * Starts a processor using the specified parallel group
     * @param group The parallel group to use with the processor
     */
    final public DataFlowProcessor start(PGroup group) {
        actor.parallelGroup = group
        actor.start()
        return this
    }

    /**
     * Stops the processor
     */
    public final void stop() { actor.stop() }

    /**
     * Joins the processor waiting for it to finish
     */
    public final void join() { actor.join() }

    /**
     * Used by the processor's body to send a value to the given output channel
     */
    final void bindOutput(final int idx, final value) {
        actor.outputs[idx] << value
    }

    /**
     * Used by the processor's body to send a value to the first / only output channel
     */
    final void bindOutput(final value) { bindOutput 0, value }

    /**
     * Used by the processor's body to send a value to the first / only output channel
     */
    @SuppressWarnings("GroovySynchronizedMethod")
    final synchronized void bindAllOutputs(final value) { outputs.each {it << value} }

    /**
     * The processor's output channel of the given index
     */
    public final getOutputs(int idx) { actor.outputs[idx] }

    /**
     * The processor's output channel of the given index
     */
    public final getOutputs() { actor.outputs }

    /**
     * The processor's first / only output channel
     */
    public final getOutput() { actor.outputs[0] }

    /**
     * Is invoked in case the actor throws an exception.
     */
    protected abstract void reportError(Throwable e)

    ;
}

protected abstract class DataFlowProcessorActor extends DynamicDispatchActor {
    protected final List inputs
    protected final List outputs
    protected final Closure code
    private final def owningProcessor

    def DataFlowProcessorActor(owningProcessor, group, outputs, inputs, code) {
        super(null)
        parallelGroup = group

        this.owningProcessor = owningProcessor
        this.outputs = outputs
        this.inputs = inputs
        this.code = code
    }

    abstract void onMessage(def message)

    ;

    final void afterStart() {
        queryInputs()
    }

    protected final def queryInputs() {
        return inputs.eachWithIndex {input, index -> input.getValAsync(index, this)}
    }

    final reportException(Throwable e) {
        owningProcessor.reportError(e)
    }
}
