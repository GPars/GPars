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

package groovyx.gpars.dataflow;

import groovy.lang.Closure;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.dataflow.operator.DataFlowProcessor;
import groovyx.gpars.group.PGroup;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Contains factory methods to create dataflow actors and starting them.
 *
 * @author Vaclav Pech, Dierk Koenig
 *         Date: Jun 4, 2009
 */
@SuppressWarnings({"rawtypes", "RawUseOfParameterizedType", "AbstractClassWithoutAbstractMethods", "AbstractClassNeverImplemented", "ConstantDeclaredInAbstractClass", "UtilityClass", "unchecked"})
public abstract class DataFlow {

    /**
     * The parallel group used by all Dataflow Concurrency actors by default.
     */
    public static final DataFlowPGroup DATA_FLOW_GROUP = new DataFlowPGroup(1);

    /**
     * Maps threads/tasks to parallel groups they belong to
     */
    public static final ThreadLocal<PGroup> activeParallelGroup = new ThreadLocal<PGroup>();

    /**
     * Retrieves the thread-local value of the active PGroup or the default DataFlowGroup
     *
     * @return The PGroup to use for DF within the current thread
     */
    public static PGroup retrieveCurrentDFPGroup() {
        PGroup pGroup = activeParallelGroup.get();
        if (pGroup == null) {
            pGroup = DataFlow.DATA_FLOW_GROUP;
        }
        return pGroup;
    }

    /**
     * Creates a new instance of SingleRunActor to run the supplied code.
     * In general cases prefer task() instead, which is more lightweight.
     *
     * @param code The actor's body
     * @return A started instance
     */
    public static Actor start(final Closure code) {
        final SingleRunActor singleRunActor = new SingleRunActor();
        singleRunActor.setBody(code);
        singleRunActor.start();
        return singleRunActor;
    }

    /**
     * Creates a new task assigned to a thread from the default dataflow parallel group.
     * Tasks are a lightweight version of dataflow operators, which do not define their communication channels explicitly,
     * but can only exchange data using explicit DataFlowVariables and Streams.
     *
     * @param code The task body to run
     * @return A DataFlowVariable, which gets assigned the value returned from the supplied code
     */
    public static DataFlowVariable task(final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.task(code);
    }

    /**
     * Creates a new task assigned to a thread from the current parallel group.
     * Tasks are a lightweight version of dataflow operators, which do not define their communication channels explicitly,
     * but can only exchange data using explicit DataFlowVariables and Streams.
     * Registers itself with DataFlow for nested 'whenBound' handlers to use the same group.
     *
     * @param callable The task body to run
     * @return A DataFlowVariable, which gets assigned the value returned from the supplied code
     */
    public static DataFlowVariable task(final Callable callable) {
        return DataFlow.DATA_FLOW_GROUP.task(callable);
    }

    /**
     * Creates a new task assigned to a thread from the current parallel group.
     * Tasks are a lightweight version of dataflow operators, which do not define their communication channels explicitly,
     * but can only exchange data using explicit DataFlowVariables and Streams.
     * Registers itself with DataFlow for nested 'whenBound' handlers to use the same group.
     *
     * @param runnable The task body to run
     * @return A DataFlowVariable, which gets bound to null once the supplied code finishes
     */
    public static DataFlowVariable task(final Runnable runnable) {
        return DataFlow.DATA_FLOW_GROUP.task(runnable);
    }

    /**
     * Creates an operator using the default dataflow parallel group
     *
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowQueue or DataFlowVariable classes) to use for inputs and outputs
     * @param code     The operator's body to run each time all inputs have a value to read
     * @return A new active operator instance
     */
    public static DataFlowProcessor operator(final Map channels, final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.operator(channels, code);
    }

    /**
     * Creates an operator using the current parallel group
     *
     * @param inputChannels  dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @param code           The operator's body to run each time all inputs have a value to read
     * @return A new active operator instance
     */
    public static DataFlowProcessor operator(final List inputChannels, final List outputChannels, final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.operator(inputChannels, outputChannels, code);
    }

    /**
     * Creates an operator using the current parallel group
     *
     * @param inputChannels  dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @param maxForks       Number of parallel threads running operator's body, defaults to 1
     * @param code           The operator's body to run each time all inputs have a value to read
     * @return A new active operator instance
     */
    public static DataFlowProcessor operator(final List inputChannels, final List outputChannels, final int maxForks, final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.operator(inputChannels, outputChannels, maxForks, code);
    }

    /**
     * Creates an operator using the current parallel group
     *
     * @param input  a dataflow channel to use for input
     * @param output a dataflow channel to use for output
     * @param code   The operator's body to run each time all inputs have a value to read
     * @return A new active operator instance
     */
    public static DataFlowProcessor operator(final DataFlowChannel input, final DataFlowChannel output, final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.operator(input, output, code);
    }

    /**
     * Creates an operator using the current parallel group
     *
     * @param input    a dataflow channel to use for input
     * @param output   a dataflow channel to use for output
     * @param maxForks Number of parallel threads running operator's body, defaults to 1
     * @param code     The operator's body to run each time all inputs have a value to read
     * @return A new active operator instance
     */
    public static DataFlowProcessor operator(final DataFlowChannel input, final DataFlowChannel output, final int maxForks, final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.operator(input, output, maxForks, code);
    }

    /**
     * Creates a selector using the default dataflow parallel group
     *
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowQueue or DataFlowVariable classes) to use for inputs and outputs
     * @param code     The selector's body to run each time a value is available in any of the inputs channels
     * @return A new active selector instance
     */
    public static DataFlowProcessor selector(final Map channels, final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.selector(channels, code);
    }

    /**
     * Creates a selector using the default dataflow parallel group
     *
     * @param inputChannels  dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @param code           The selector's body to run each time a value is available in any of the inputs channels
     * @return A new active selector instance
     */
    public static DataFlowProcessor selector(final List inputChannels, final List outputChannels, final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.selector(inputChannels, outputChannels, code);
    }

    /**
     * Creates a selector using the default dataflow parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     *
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowQueue or DataFlowVariable classes) to use for inputs and outputs
     * @return A new active selector instance
     */
    public static DataFlowProcessor selector(final Map channels) {
        return DataFlow.DATA_FLOW_GROUP.selector(channels);
    }

    /**
     * Creates a selector using the default dataflow parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     *
     * @param inputChannels  dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @return A new active selector instance
     */
    public static DataFlowProcessor selector(final List inputChannels, final List outputChannels) {
        return DataFlow.DATA_FLOW_GROUP.selector(inputChannels, outputChannels);
    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group
     * Input with lower position index have higher priority.
     *
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowQueue or DataFlowVariable classes) to use for inputs and outputs
     * @param code     The selector's body to run each time a value is available in any of the inputs channels
     * @return A new active selector instance
     */
    public static DataFlowProcessor prioritySelector(final Map channels, final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.prioritySelector(channels, code);
    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group
     * Input with lower position index have higher priority.
     *
     * @param inputChannels  dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @param code           The selector's body to run each time a value is available in any of the inputs channels
     * @return A new active selector instance
     */
    public static DataFlowProcessor prioritySelector(final List inputChannels, final List outputChannels, final Closure code) {
        return DataFlow.DATA_FLOW_GROUP.prioritySelector(inputChannels, outputChannels, code);
    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     * Input with lower position index have higher priority.
     *
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowQueue or DataFlowVariable classes) to use for inputs and outputs
     * @return A new active selector instance
     */
    public static DataFlowProcessor prioritySelector(final Map channels) {
        return DataFlow.DATA_FLOW_GROUP.prioritySelector(channels);
    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     * Input with lower position index have higher priority.
     *
     * @param inputChannels  dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @return A new active selector instance
     */
    public static DataFlowProcessor prioritySelector(final List inputChannels, final List outputChannels) {
        return DataFlow.DATA_FLOW_GROUP.prioritySelector(inputChannels, outputChannels);
    }

    /**
     * Creates a splitter copying its single input channel into all of its output channels. The created splitter will be part of the default parallel group
     * Input with lower position index have higher priority.
     *
     * @param inputChannel   The channel to  read values from
     * @param outputChannels A list of channels to output to
     * @return A new active splitter instance
     */
    public static DataFlowProcessor splitter(final DataFlowChannel inputChannel, final List<DataFlowWriteChannel> outputChannels) {
        return DataFlow.DATA_FLOW_GROUP.splitter(inputChannel, outputChannels);
    }

    /**
     * Creates a splitter copying its single input channel into all of its output channels. The created splitter will be part of this parallel group
     * Input with lower position index have higher priority.
     *
     * @param inputChannel   The channel to  read values from
     * @param outputChannels A list of channels to output to
     * @param maxForks       Number of threads running the splitter's body, defaults to 1
     * @return A new active splitter instance
     */
    public static DataFlowProcessor splitter(final DataFlowChannel inputChannel, final List<DataFlowChannel> outputChannels, final int maxForks) {
        return DataFlow.DATA_FLOW_GROUP.splitter(inputChannel, outputChannels, maxForks);
    }

    /**
     * Creates a select using the default dataflow parallel group. The returns Select instance will allow the user to
     * obtain values from the supplied dataflow variables or streams as they become available.
     *
     * @param channels Dataflow variables or streams to wait for values on
     * @return A new select instance
     */
    public static Select<?> select(final DataFlowChannel<?>... channels) {
        return DataFlow.DATA_FLOW_GROUP.select(channels);
    }
}
