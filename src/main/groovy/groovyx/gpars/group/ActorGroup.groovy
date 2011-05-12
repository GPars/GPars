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

package groovyx.gpars.group

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.actor.ReactiveActor
import groovyx.gpars.actor.impl.RunnableBackedBlockingActor
import groovyx.gpars.agent.Agent
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.dataflow.Select
import groovyx.gpars.dataflow.operator.DataflowOperator
import groovyx.gpars.dataflow.operator.DataflowPrioritySelector
import groovyx.gpars.dataflow.operator.DataflowProcessor
import groovyx.gpars.dataflow.operator.DataflowSelector
import groovyx.gpars.scheduler.Pool
import java.util.concurrent.Callable
import groovyx.gpars.actor.BlockingActor

/**
 * Provides a common super class of pooled parallel groups.
 *
 * @author Vaclav Pech, Alex Tkachman
 * Date: May 8, 2009
 */
public abstract class PGroup {

    /**
     * Stores the group actors' thread pool
     */
    private @Delegate Pool threadPool

    public Pool getThreadPool() { return threadPool; }

    /**
     * Creates a group for actors, agents, tasks and operators. The actors will share a common daemon thread pool.
     */
    protected def PGroup(final Pool threadPool) {
        this.threadPool = threadPool
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in runnable/closure as the body of the actor's act() method.
     * The created actor will belong to the pooled parallel group.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the DefaultActor class
     */
    public final DefaultActor actor(Runnable handler) {
        final DefaultActor actor = new DefaultActor(handler)
        actor.parallelGroup = this
        actor.start()
        return actor
    }

    /**
     * Creates a new instance of BlockingActor, using the passed-in closure as the body of the actor's act() method.
     * The created actor will be part of the default actor group.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the BlockingActor class
     */
    public final BlockingActor blockingActor(Runnable handler) {
        final BlockingActor actor = new RunnableBackedBlockingActor(handler)
        actor.parallelGroup = this
        actor.start()
        return actor
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in runnable/closure as the body of the actor's act() method.
     * The actor will cooperate in thread sharing with other actors sharing the same thread pool in a fair manner.
     * The created actor will belong to the pooled parallel group.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the DefaultActor class
     */
    public final DefaultActor fairActor(Runnable handler) {
        final DefaultActor actor = new DefaultActor(handler)
        actor.parallelGroup = this
        actor.makeFair()
        actor.start()
        return actor
    }

    /**
     * Creates a reactor around the supplied code.
     * When a reactor receives a message, the supplied block of code is run with the message
     * as a parameter and the result of the code is send in reply.
     * @param The code to invoke for each received message
     * @return A new instance of ReactiveEventBasedThread
     */
    public final Actor reactor(final Closure code) {
        final def actor = new ReactiveActor(code)
        actor.parallelGroup = this
        actor.start()
        actor
    }

    /**
     * Creates a reactor around the supplied code, which will cooperate in thread sharing with other actors sharing the same thread pool
     * When a reactor receives a message, the supplied block of code is run with the message
     * as a parameter and the result of the code is send in reply.
     * @param The code to invoke for each received message
     * @return A new instance of ReactiveEventBasedThread
     */
    public final Actor fairReactor(final Closure code) {
        final def actor = new ReactiveActor(code)
        actor.parallelGroup = this
        actor.makeFair()
        actor.start()
        actor
    }

    /**
     * Creates an instance of DynamicDispatchActor.
     * @param code The closure specifying individual message handlers.
     */
    public final Actor messageHandler(final Closure code) {
        final def actor = new DynamicDispatchActor().become(code)
        actor.parallelGroup = this
        actor.start()
        actor
    }

    /**
     * Creates an instance of DynamicDispatchActor, which will cooperate in thread sharing with other actors sharing the same thread pool
     * @param code The closure specifying individual message handlers.
     */
    public final Actor fairMessageHandler(final Closure code) {
        final def actor = new DynamicDispatchActor().become(code)
        actor.parallelGroup = this
        actor.makeFair()
        actor.start()
        actor
    }

    /**
     * Creates an agent instance initialized with the given state
     * @param state The initial internal state of the new Agent instance
     * @return The created instance
     */
    public final <T> Agent<T> agent(final T state) {
        final Agent safe = new Agent(state)
        safe.attachToThreadPool threadPool
        return safe
    }

    /**
     * Creates an agent instance initialized with the given state
     * @param state The initial internal state of the new Agent instance
     * @param copy A closure to use to create a copy of the internal state when sending the internal state out
     * @return The created instance
     */
    public final <T> Agent<T> agent(final T state, final Closure copy) {
        final Agent safe = new Agent(state, copy)
        safe.attachToThreadPool threadPool
        return safe
    }

    /**
     * Creates an agent instance initialized with the given state, which will cooperate in thread sharing with other agents and actors in a fair manner.
     * @param state The initial internal state of the new Agent instance
     * @return The created instance
     */
    public final <T> Agent<T> fairAgent(final T state) {
        final Agent safe = agent(state)
        safe.makeFair()
        return safe
    }

    /**
     * Creates an agent instance initialized with the given state, which will cooperate in thread sharing with other agents and actors in a fair manner.
     * @param copy A closure to use to create a copy of the internal state when sending the internal state out
     * @param state The initial internal state of the new Agent instance
     * @return The created instance
     */
    public final <T> Agent<T> fairAgent(final T state, final Closure copy) {
        final Agent safe = agent(state, copy)
        safe.makeFair()
        return safe
    }

    /**
     * Creates a new task assigned to a thread from the current parallel group.
     * Tasks are a lightweight version of dataflow operators, which do not define their communication channels explicitly,
     * but can only exchange data using explicit DataflowVariables and Streams.
     * Registers itself with Dataflow for nested 'whenBound' handlers to use the same group.
     * @param code The task body to run
     * @return A DataflowVariable, which gets assigned the value returned from the supplied code
     */
    public DataflowVariable task(final Closure code) {
        return task(code.clone() as Callable)
    }

    /**
     * Creates a new task assigned to a thread from the current parallel group.
     * Tasks are a lightweight version of dataflow operators, which do not define their communication channels explicitly,
     * but can only exchange data using explicit DataflowVariables and Streams.
     * Registers itself with Dataflow for nested 'whenBound' handlers to use the same group.
     * @param callable The task body to run
     * @return A DataflowVariable, which gets assigned the value returned from the supplied code
     */
    public DataflowVariable task(final Callable callable) {
        final DataflowVariable result = new DataflowVariable()
        threadPool.execute {->
            Dataflow.activeParallelGroup.set this
            try {
                result.bind callable.call()
            } finally {
                Dataflow.activeParallelGroup.remove()
            }
        }
        return result
    }

    /**
     * Creates a new task assigned to a thread from the current parallel group.
     * Tasks are a lightweight version of dataflow operators, which do not define their communication channels explicitly,
     * but can only exchange data using explicit DataflowVariables and Streams.
     * Registers itself with Dataflow for nested 'whenBound' handlers to use the same group.
     * @param code The task body to run
     * @return A DataflowVariable, which gets bound to null once the supplied code finishes
     */
    public DataflowVariable task(final Runnable code) {
        if (code instanceof Closure) return task(code.clone() as Callable)
        final DataflowVariable result = new DataflowVariable()
        threadPool.execute {->
            Dataflow.activeParallelGroup.set this
            try {
                code.run()
                result.bind null
            } finally {
                Dataflow.activeParallelGroup.remove()
            }
        }
        return result
    }

    /**
     * Creates an operator using the current parallel group
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataflowQueue or DataflowVariable classes) to use for inputs and outputs
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public DataflowProcessor operator(final Map channels, final Closure code) {
        return new DataflowOperator(this, channels, code).start()
    }

    /**
     * Creates an operator using the current parallel group
     *
     * @param inputChannels dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @param code The operator's body to run each time all inputs have a value to read
     * @return A new active operator instance
     */
    public DataflowProcessor operator(final List inputChannels, final List outputChannels, final Closure code) {
        return new DataflowOperator(this, [inputs: inputChannels, outputs: outputChannels], code).start()
    }

    /**
     * Creates an operator using the current parallel group
     *
     * @param inputChannels dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @param maxForks Number of parallel threads running operator's body, defaults to 1
     * @param code The operator's body to run each time all inputs have a value to read
     * @return A new active operator instance
     */
    public DataflowProcessor operator(final List inputChannels, final List outputChannels, final int maxForks, final Closure code) {
        return new DataflowOperator(this, [inputs: inputChannels, outputs: outputChannels, maxForkd: maxForks], code).start()
    }

    /**
     * Creates an operator using the current parallel group
     * @param input a dataflow channel to use for input
     * @param output a dataflow channel to use for output
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public DataflowProcessor operator(final DataflowReadChannel input, final DataflowWriteChannel output, final Closure code) {
        return new DataflowOperator(this, [inputs: [input], outputs: [output]], code).start()
    }

    /**
     * Creates an operator using the current parallel group
     * @param input a dataflow channel to use for input
     * @param output a dataflow channel to use for output
     * @param maxForks Number of parallel threads running operator's body, defaults to 1
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public DataflowProcessor operator(final DataflowReadChannel input, final DataflowWriteChannel output, final int maxForks, final Closure code) {
        return new DataflowOperator(this, [inputs: [input], outputs: [output], maxForkd: maxForks], code).start()
    }

    /**
     * Creates a selector using this parallel group
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataflowQueue or DataflowVariable classes) to use for inputs and outputs
     * @param code The selector's body to run each time a value is available in any of the inputs channels
     */
    public DataflowProcessor selector(final Map channels, final Closure code) {
        return new DataflowSelector(this, channels, code).start()
    }

    /**
     * Creates a selector using this parallel group
     * @param inputChannels dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @param code The selector's body to run each time a value is available in any of the inputs channels
     */
    public DataflowProcessor selector(final List inputChannels, final List outputChannels, final Closure code) {
        return new DataflowSelector(this, [inputs: inputChannels, outputs: outputChannels], code).start()
    }

    /**
     * Creates a selector using this parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataflowQueue or DataflowVariable classes) to use for inputs and outputs
     * @param code The selector's body to run each time a value is available in any of the inputs channels
     */
    public DataflowProcessor selector(final Map channels) {
        return new DataflowSelector(this, channels, {bindAllOutputsAtomically it}).start()
    }

    /**
     * Creates a selector using this parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     * @param inputChannels dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @param code The selector's body to run each time a value is available in any of the inputs channels
     */
    public DataflowProcessor selector(final List inputChannels, final List outputChannels) {
        return new DataflowSelector(this, [inputs: inputChannels, outputs: outputChannels], {bindAllOutputsAtomically it}).start()
    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group
     * Input with lower position index have higher priority.
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataflowQueue or DataflowVariable classes) to use for inputs and outputs
     * @param code The selector's body to run each time a value is available in any of the inputs channels
     */
    public DataflowProcessor prioritySelector(final Map channels, final Closure code) {
        return new DataflowPrioritySelector(this, channels, code).start()
    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group
     * Input with lower position index have higher priority.
     * @param inputChannels dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     * @param code The selector's body to run each time a value is available in any of the inputs channels
     */
    public DataflowProcessor prioritySelector(final List inputChannels, final List outputChannels, final Closure code) {
        return new DataflowPrioritySelector(this, [inputs: inputChannels, outputs: outputChannels], code).start()
    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     * Input with lower position index have higher priority.
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataflowQueue or DataflowVariable classes) to use for inputs and outputs
     */
    public DataflowProcessor prioritySelector(final Map channels) {
        return new DataflowPrioritySelector(this, channels, {bindAllOutputsAtomically it}).start()
    }

    /**
     * Creates a prioritizing selector using the default dataflow parallel group. Since no body is provided, the selector will simply copy the incoming values to all output channels.
     * Input with lower position index have higher priority.
     * @param inputChannels dataflow channels to use for input
     * @param outputChannels dataflow channels to use for output
     */
    public DataflowProcessor prioritySelector(final List inputChannels, final List outputChannels) {
        return new DataflowPrioritySelector(this, [inputs: inputChannels, outputs: outputChannels], {bindAllOutputsAtomically it}).start()
    }

    /**
     * Creates a splitter copying its single input channel into all of its output channels. The created splitter will be part of this parallel group
     * Input with lower position index have higher priority.
     * @param inputChannel The channel to  read values from
     * @param outputChannels A list of channels to output to
     */
    public DataflowProcessor splitter(final DataflowReadChannel inputChannel, final List<DataflowWriteChannel> outputChannels) {
        if (inputChannel == null || !outputChannels) throw new IllegalArgumentException("A splitter needs an input channel and at keast one output channel to be created.")
        return new DataflowOperator(this, [inputs: [inputChannel], outputs: outputChannels], {bindAllOutputs it}).start()
    }

    /**
     * Creates a splitter copying its single input channel into all of its output channels. The created splitter will be part of this parallel group
     * Input with lower position index have higher priority.
     * @param inputChannel The channel to  read values from
     * @param outputChannels A list of channels to output to
     * @param maxForks Number of threads running the splitter's body, defaults to 1
     */
    public DataflowProcessor splitter(final DataflowReadChannel inputChannel, final List<DataflowWriteChannel> outputChannels, int maxForks) {
        if (inputChannel == null || !outputChannels) throw new IllegalArgumentException("A splitter needs an input channel and at keast one output channel to be created.")
        return new DataflowOperator(this, [inputs: [inputChannel], outputs: outputChannels, maxForks: maxForks], {bindAllOutputsAtomically it}).start()
    }

    /**
     * Creates a select using the current parallel group. The returns Select instance will allow the user to
     * obtain values from the supplied dataflow variables or streams as they become available.
     * @param channels Dataflow variables or streams to wait for values on
     */
    public Select select(final DataflowReadChannel... channels) {
        return new Select(this, channels)
    }

    /**
     * Creates a select using the current parallel group. The returns Select instance will allow the user to
     * obtain values from the supplied dataflow variables or streams as they become available.
     * @param channels Dataflow variables or streams to wait for values on
     */
    public Select select(final List<DataflowReadChannel> channels) {
        return new Select(this, channels)
    }

    /**
     * Shutdown the thread pool gracefully
     */
    protected void finalize() {
        this.threadPool.shutdown()
        super.finalize()
    }
}
