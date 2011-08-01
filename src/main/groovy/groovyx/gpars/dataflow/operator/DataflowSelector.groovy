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

import groovyx.gpars.dataflow.Select
import groovyx.gpars.group.PGroup
import java.util.concurrent.Semaphore

/**
 * Dataflow selectors and operators (processors) form the basic units in dataflow networks. They are typically combined into oriented graphs that transform data.
 * They accept a set of input and output dataflow channels so that once values are available to be consumed in any
 * of the input channels the selector's body is triggered on the values, potentially generating values to be written into the output channels.
 * The output channels at the same time are suitable to be used as input channels by some other dataflow processors.
 * The channels allow processors to communicate.
 *
 * Dataflow selectors and operators enable creation of highly concurrent applications yet the abstraction hides the low-level concurrency primitives
 * and exposes much friendlier API.
 * Since selectors and operators internally leverage the actor implementation, they reuse a pool of threads and so the actual number of threads
 * used by the calculation can be kept much lower than the actual number of processors used in the network.
 *
 * Selectors select a random value from the values available in the input channels. Optionally the selector's guards mask
 * can be altered to limit the number of channels considered for selection.
 *
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */
public class DataflowSelector extends DataflowProcessor {

    protected final Select select
    protected final List<Boolean> guards

    /**
     * Creates a selector
     * After creation the selector needs to be started using the start() method.
     * @param group A parallel group to use threads from in the internal actor
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataflowQueue or DataflowVariable classes) to use for inputs and outputs
     * @param code The selector's body to run each time all inputs have a value to read
     */
    protected def DataflowSelector(final PGroup group, final Map channels, final Closure code) {
        super(channels, code)
        final int parameters = code.maximumNumberOfParameters
        if (verifyChannelParameters(channels, parameters))
            throw new IllegalArgumentException("The selector's body must accept 1 or two parameters, while it currently requests ${parameters} parameters.")
        final def inputs = channels.inputs.asImmutable()
        final def outputs = channels.outputs?.asImmutable()

        if (shouldBeMultiThreaded(channels)) {
            if (channels.maxForks < 1) throw new IllegalArgumentException("The maxForks argument must be a positive value. ${channels.maxForks} was provided.")
            this.actor = new ForkingDataflowSelectorActor(this, group, outputs, inputs, code.clone(), channels.maxForks)
        } else {
            this.actor = new DataflowSelectorActor(this, group, outputs, inputs, code.clone())
        }
        select = new Select(group, inputs)
        guards = Collections.synchronizedList(new ArrayList<Boolean>((int) inputs.size()))
        //fill in the provided or default guard flags
        if (channels.guards) {
            channels.guards.eachWithIndex {flag, index -> guards[index] = flag}
        } else {
            for (int i = 0; i < inputs.size(); i++) guards.add(Boolean.TRUE)
        }
    }

    private boolean verifyChannelParameters(Map channels, int parameters) {
        return !channels || (channels.inputs == null) || !(parameters in [1, 2])
    }

    /**
     * Used to enable/disable individual input channels from next selections
     * @param index The index of the channel to enable/disable
     * @param flag True, if the channel should be included in selection, false otherwise
     */
    public final void setGuard(int index, boolean flag) {
        guards[index] = flag
    }

    /**
     * Used to enable/disable individual input channels from next selections
     * @param index The index of the channel to enable/disable
     * @param flag True, if the channel should be included in selection, false otherwise
     */
    public final void setGuards(List<Boolean> flags) {
        flags.eachWithIndex {flag, int index -> guards[index] = flag}
    }

    /**
     * Ask for another select operation on the internal select instance.
     * The selector's guards are applied to the selection.
     */
    protected void doSelect() {
        select(this.actor, guards)
    }
}

/**
 * An selector's internal actor. Repeatedly polls inputs and once they're all available it performs the selector's body.
 *
 * Iteratively waits for enough values from inputs.
 * Once all required inputs are available (received as messages), the selector's body is run.
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

/**
 * An selector's internal actor. Repeatedly polls inputs and once they're all available it performs the selector's body.
 * The selector's body is executed in as a separate task, allowing multiple copies of the body to be run concurrently.
 * The maxForks property guards the maximum number or concurrently run copies.
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

