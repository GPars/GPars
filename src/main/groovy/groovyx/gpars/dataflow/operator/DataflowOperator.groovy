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

import groovyx.gpars.group.PGroup

/**
 * Dataflow selectors and operators (processors) form the basic units in dataflow networks. They are typically combined into oriented graphs that transform data.
 * They accept a set of input and output dataflow channels so that once values are available to be consumed in all
 * the input channels the operator's body is triggered on the values, potentially generating values to be written into the output channels.
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
public final class DataflowOperator extends DataflowProcessor {

    /**
     * Creates an operator
     * After creation the operator needs to be started using the start() method.
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataflowQueue or DataflowVariable classes) to use for inputs and outputs
     * @param code The operator's body to run each time all inputs have a value to read
     */
    def DataflowOperator(final PGroup group, final Map channels, final Closure code) {
        super(channels, code)
        final int parameters = code.maximumNumberOfParameters
        if (verifyChannelParameters(channels, parameters))
            throw new IllegalArgumentException("The operator's body accepts $parameters parameters while it is given ${channels?.inputs?.size()} input streams. The numbers must match.")
        if (shouldBeMultiThreaded(channels)) {
            if (channels.maxForks < 1) throw new IllegalArgumentException("The maxForks argument must be a positive value. ${channels.maxForks} was provided.")
            this.actor = new ForkingDataflowOperatorActor(this, group, channels.outputs?.asImmutable(), channels.inputs.asImmutable(), code.clone(), channels.maxForks)
        } else {
            this.actor = new DataflowOperatorActor(this, group, channels.outputs?.asImmutable(), channels.inputs.asImmutable(), code.clone())
        }
    }

    private boolean verifyChannelParameters(Map channels, int parameters) {
        return !channels || (channels.inputs == null) || (parameters != channels.inputs.size())
    }
}

