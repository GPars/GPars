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

package groovyx.gpars.group

import groovyx.gpars.actor.AbstractPooledActor
import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.actor.ReactiveActor
import groovyx.gpars.actor.impl.RunnableBackedPooledActor
import groovyx.gpars.agent.Agent
import groovyx.gpars.dataflow.DataFlowExpression
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.dataflow.operator.DataFlowOperator
import groovyx.gpars.scheduler.Pool

/**
 * Provides a common super class of pooled actor groups.
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
     * Creates a new instance of PooledActor, using the passed-in runnable/closure as the body of the actor's act() method.
     * The created actor will belong to the pooled actor group.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the AbstractPooledActor class
     */
    public final AbstractPooledActor actor(Runnable handler) {
        final AbstractPooledActor actor = new RunnableBackedPooledActor(handler)
        actor.parallelGroup = this
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
     * Creates a reactor around the supplied code, which will cooperate in thread sharing with other Agent instances
     * in a fair manner.
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
        final def actor = new DynamicDispatchActor(code)
        actor.parallelGroup = this
        actor.start()
        actor
    }

    /**
     * Creates an instance of DynamicDispatchActor, which will cooperate in thread sharing with other Agent instances
     * in a fair manner.
     * @param code The closure specifying individual message handlers.
     */
    public final Actor fairMessageHandler(final Closure code) {
        final def actor = new DynamicDispatchActor(code)
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
    public final Agent agent(final def state) {
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
    public final Agent agent(final def state, final Closure copy) {
        final Agent safe = new Agent(state, copy)
        safe.attachToThreadPool threadPool
        return safe
    }

    /**
     * Creates an agent instance initialized with the given state, which will cooperate in thread sharing with other Agent instances
     * in a fair manner.
     * @param state The initial internal state of the new Agent instance
     * @return The created instance
     */
    public final Agent fairAgent(final def state) {
        final Agent safe = agent(state)
        safe.makeFair()
        return safe
    }

    /**
     * Creates an agent instance initialized with the given state, which will cooperate in thread sharing with other Agent instances
     * in a fair manner.
     * @param copy A closure to use to create a copy of the internal state when sending the internal state out
     * @param state The initial internal state of the new Agent instance
     * @return The created instance
     */
    public final Agent fairAgent(final def state, final Closure copy) {
        final Agent safe = agent(state, copy)
        safe.makeFair()
        return safe
    }

    /**
     * Creates a new task assigned to a thread from the current actor group.
     * Tasks are a lightweight version of dataflow operators, which do not define their communication channels explicitly,
     * but can only exchange data using explicit DataFlowVariables and Streams.
     * Registers itself with DataFlowExpressions for nested 'whenBound' handlers to use the same group.
     * @param code The task body to run
     * @return A DataFlowVariable, which gets assigned the value returned from the supplied code
     */
    public DataFlowVariable task(final Closure code) {
        final DataFlowVariable result = new DataFlowVariable()
        def cloned = code.clone()
        threadPool.execute {->
            DataFlowExpression.activeParallelGroup.set this
            try {
                result.bind cloned()
            } finally {
                DataFlowExpression.activeParallelGroup.remove()
            }
        }
        return result
    }

    /**
     * Creates an operator using the current actor group
     * @param channels A map specifying "inputs" and "outputs" - dataflow channels (instances of the DataFlowStream or DataFlowVariable classes) to use for inputs and outputs
     * @param code The operator's body to run each time all inputs have a value to read
     */
    public DataFlowOperator operator(final Map channels, final Closure code) {
        return new DataFlowOperator(channels, code).start(this)
    }
}
