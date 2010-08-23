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
import groovyx.gpars.dataflow.operator.DataFlowOperator

/**
 * Contains factory methods to create dataflow actors and starting them.
 *
 * @author Vaclav Pech, Dierk Koenig
 * Date: Jun 4, 2009
 */
public abstract class DataFlow {

    /**
     * The actor group used by all Dataflow Concurrency actors by default.
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
     * Creates a new task assigned to a thread from the default dataflow actor group.
     * Tasks are a lightweight version of dataflow operators, which do not define their communication channels explicitly,
     * but can only exchange data using explicit DataFlowVariables and Streams.
     * @param code The task body to run
     * @return A DataFlowVariable, which gets assigned the value returned from the supplied code
     */
    public static DataFlowVariable task(final Closure code) {
        DataFlow.DATA_FLOW_GROUP.task code
    }

    /**
     * Creates an operator using the default operator actor group
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public static DataFlowOperator operator(final Map channels, final Closure code) {
        DataFlow.DATA_FLOW_GROUP.operator(channels, code)
    }
}
