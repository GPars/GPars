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

package groovyx.gpars.dataflow

import groovyx.gpars.actor.Actor
import groovyx.gpars.dataflow.operator.DataFlowProcessor

/**
 * Contains factory methods to create dataflow actors and starting them.
 *
 * @author Vaclav Pech, Dierk Koenig
 * Date: Jun 4, 2009
 */
public abstract class DataFlow {

    /**
     * The parallel group used by all Dataflow Concurrency actors by default.
     */
    public static final DataFlowPGroup DATA_FLOW_GROUP = new DataFlowPGroup(1)

    /**
     * Tasks need no channels
     */
    private static def taskChannels = [inputs: [], outputs: []]

    /**
     * Creates a new instance of SingleRunActor to run the supplied code.
     * In general cases prefer task() instead, which is more lightweight.
     */
    public static Actor start(final Closure code) {
        new SingleRunActor(body: code).start()
    }

    /**
     * Creates a new task assigned to a thread from the default dataflow parallel group.
     * Tasks are a lightweight version of dataflow operators, which do not define their communication channels explicitly,
     * but can only exchange data using explicit DataFlowVariables and Streams.
     * @param code The task body to run
     * @return A DataFlowVariable, which gets assigned the value returned from the supplied code
     */
    public static DataFlowVariable task(final Closure code) {
        DataFlow.DATA_FLOW_GROUP.task code
    }

    /**
     * Creates an operator using the default dataflow parallel group
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public static DataFlowProcessor operator(final Map channels, final Closure code) {
        DataFlow.DATA_FLOW_GROUP.operator(channels, code)

    }

    /**
     * Creates an operator using the current parallel group
     * @param input a dataflow channel to use for input
     * @param output a dataflow channel to use for output
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public static DataFlowProcessor operator(final DataFlowChannel input, final DataFlowChannel output, final Closure code) {
        DataFlow.DATA_FLOW_GROUP.operator(input, output, code)
    }

    /**
     * Creates an operator using the current parallel group
     * @param input a dataflow channel to use for input
     * @param output a dataflow channel to use for output
     * @param maxForks Number of parallel threads running operator's body, defaults to 1
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public static DataFlowProcessor operator(final DataFlowChannel input, final DataFlowChannel output, final int maxForks, final Closure code) {
        DataFlow.DATA_FLOW_GROUP.operator(input, output, maxForks, code)
    }

    /**
     * Creates a selector using the default dataflow parallel group
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The selector's body to run each time a value is available in any of the inputs channels
     */
    public static DataFlowProcessor selector(final Map channels, final Closure code) {
        DataFlow.DATA_FLOW_GROUP.selector(channels, code)
    }

    /**
     * Creates a selector using the default dataflow parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     */
    public static DataFlowProcessor selector(final Map channels) {
        DataFlow.DATA_FLOW_GROUP.selector(channels)

    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group
     * Input with lower position index have higher priority.
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The selector's body to run each time a value is available in any of the inputs channels
     */
    public static DataFlowProcessor prioritySelector(final Map channels, final Closure code) {
        DataFlow.DATA_FLOW_GROUP.prioritySelector(channels, code)
    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     * Input with lower position index have higher priority.
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     */
    public static DataFlowProcessor prioritySelector(final Map channels) {
        DataFlow.DATA_FLOW_GROUP.prioritySelector(channels)

    }

    /**
     * Creates a splitter copying its single input channel into all of its output channels. The created splitter will be part of the default parallel group
     * Input with lower position index have higher priority.
     * @param inputChannel The channel to  read values from
     * @param outputChannels A list of channels to output to
     */
    public static DataFlowProcessor splitter(final DataFlowChannel inputChannel, final List<DataFlowChannel> outputChannels) {
        DataFlow.DATA_FLOW_GROUP.splitter(inputChannel, outputChannels)
    }

    /**
     * Creates a splitter copying its single input channel into all of its output channels. The created splitter will be part of this parallel group
     * Input with lower position index have higher priority.
     * @param inputChannel The channel to  read values from
     * @param outputChannels A list of channels to output to
     * @param maxForks Number of threads running the splitter's body, defaults to 1
     */
    public static DataFlowProcessor splitter(final DataFlowChannel inputChannel, final List<DataFlowChannel> outputChannels, int maxForks) {
        DataFlow.DATA_FLOW_GROUP.splitter(inputChannel, outputChannels, maxForks)
    }

    /**
     * Creates a select using the default dataflow parallel group. The returns Select instance will allow the user to
     * obtain values from the supplied dataflow variables or streams as they become available.
     * @param channels Dataflow variables or streams to wait for values on
     */
    public static Select select(final DataFlowChannel... channels) {
        DataFlow.DATA_FLOW_GROUP.select(channels)
    }

    /**
     * Creates a priority select using the default dataflow parallel group. The returns PrioritySelect instance will allow the user to
     * obtain values from the supplied dataflow variables or streams as they become available, prioritizing by the channel index,
     * giving lower index higher priority.
     * @param channels Dataflow variables or streams to wait for values on, with priority decreasing with increasing index value
     */
    public static PrioritySelect prioritySelect(final DataFlowChannel... channels) {
        DataFlow.DATA_FLOW_GROUP.prioritySelect(channels)
    }

    /**
     * Creates a priority select using the default dataflow parallel group. The returns PrioritySelect instance will allow the user to
     * obtain values from the supplied dataflow variables or streams as they become available, prioritizing by the channel index,
     * giving lower index higher priority.
     * @param channels Dataflow variables or streams to wait for values on, with priority decreasing with increasing index value
     * @param itemFactory An optional factory creating items to output out of the received items and their index. The default implementation only propagates the obtained values and ignores the index
     */
    public static PrioritySelect prioritySelect(final Closure itemFactory, final DataFlowChannel... channels) {
        DataFlow.DATA_FLOW_GROUP.prioritySelect(itemFactory, channels)
    }
}
