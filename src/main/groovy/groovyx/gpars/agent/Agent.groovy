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

package groovyx.gpars.agent

import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.util.EnhancedRWLock
import java.util.concurrent.CopyOnWriteArrayList
import org.codehaus.groovy.runtime.NullObject

/**
 * A special-purpose thread-safe non-blocking reference implementation inspired by Agents in Clojure.
 * Agents safe-guard mutable values by allowing only a single agent-managed thread to make modifications to them.
 * The mutable values are not directly accessible from outside, but instead requests have to be sent to the agent
 * and the agent guarantees to process the requests sequentially on behalf of the callers. Agents guarantee sequential
 * execution of all requests and so consistency of the values. 
 * An agent wraps a reference to mutable state, held inside a single field, and accepts code (closures / commands)
 * as messages, which can be sent to the Agent just like to any other actor using the '<<' operator
 * or any of the send() methods.
 * After reception of a closure / command, the closure is invoked against the internal mutable field. The closure is guaranteed
 * to be run without intervention from other threads and so may freely alter the internal state of the Agent
 * held in the internal <i>data</i> field.
 * The return value of the submitted closure is sent in reply to the sender of the closure.
 * If the message sent to an agent is not a closure, it is considered to be a new value for the internal
 * reference field. The internal reference can also be changed using the updateValue() method from within the received
 * closures.
 * The 'val' property of an agent will safely return the current value of the Agent, while the valAsync() method
 * will do the same without blocking the caller.
 * The 'instantVal' property will retrieve the current value of the Agent without having to wait in the queue of tasks.
 * The initial internal value can be passed to the constructor. The two-parameter constructor allows to alter the way
 * the internal value is returned from val/valAsync. By default the original reference is returned, but in many scenarios a copy
 * or a clone might be more appropriate.
 *
 * @author Vaclav Pech
 * Date: Jul 2, 2009
 */
public class Agent<T> extends AgentCore {

    /**
     * Allows reads not to wait in the message queue.
     * Writes and reads are mutually separated by using write or read locks respectively.
     */
    private EnhancedRWLock lock = new EnhancedRWLock()

    /**
     * Holds the internal mutable state
     */
    protected T data

    /**
     * Function converting the internal state during read to prevent internal state escape from
     * the protected boundary of the agent
     */
    final Closure copy = {it}

    /**
     * Holds all listeners interested in state updates
     * A listener should be a closure accepting the old and the new value in this order.
     */
    final List listeners = new CopyOnWriteArrayList()

    /**
     * Holds all validators checking the agent's state
     * A validator should be a closure accepting the old and the new value in this order.
     */
    final List validators = new CopyOnWriteArrayList()

    /**
     * Creates a new Agent with the internal state set to null
     */
    def Agent() {
        this(null)
    }

    /**
     * Creates a new Agent around the supplied modifiable object
     * @param data The object to use for storing the internal state of the variable
     */
    def Agent(final T data) {
        this.data = data
    }

    /**
     * Creates a new Agent around the supplied modifiable object
     * @param data The object to use for storing the internal state of the variable
     * @param copy A closure to use to create a copy of the internal state when sending the internal state out
     */
    def Agent(final T data, final Closure copy) {
        this.data = data
        this.copy = copy
    }

    /**
     * Accepts a NullObject instance and sets the internal state to null
     */
    final void onMessage(NullObject obj) {
        lock.withWriteLock {
            updateValue null
        }
    }

    /**
     * Accepts and invokes the closure
     */
    final void onMessage(Closure code) {
        lock.withWriteLock {
            code.delegate = this
            code.call(copy(data))
        }
    }

    /**
     * Other messages than closures are accepted as new values for the internal state
     */
    final void onMessage(T message) {
        lock.withWriteLock {
            updateValue message
        }
    }

    /**
     * Allows closures to set the new internal state as a whole
     */
    final void updateValue(T newValue) {
        def oldValue = copy(data)

        def validated = false
        try {
            for (validator in validators) validator(oldValue, newValue)
            validated = true
        } catch (Exception e) {
            registerError e
        }
        if (validated) {
            data = newValue
            for (listener in listeners) listener(oldValue, newValue)
        }
    }

    /**
     * A shorthand method for safe message-based retrieval of the internal state.
     * Retrieves the internal state immediately by-passing the queue of tasks waiting to be processed.
     */
    final public T getInstantVal() {
        T result = null
        lock.withReadLock { result = copy(data) }
        return result
    }

    /**
     * A shorthand method for safe message-based retrieval of the internal state.
     * The request to retrieve a value is put into the message queue, so will wait for all messages delivered earlier to complete.
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    final public T getVal() {
        sendAndWait { copy it }
    }

    /**
     * A shorthand method for safe asynchronous message-based retrieval of the internal state.
     * The request to retrieve a value is put into the message queue, so will wait for all messages delivered earlier to complete.
     * @param callback A closure to invoke with the internal state as a parameter
     */
    final public void valAsync(Closure callback) {
        send {callback.call(copy(it))}
    }

    /**
     * Submits the closure waiting for the result
     */
    final def sendAndWait(Closure message) {
        final DataFlowVariable result = new DataFlowVariable()
        this.send {
            result.bind message.call(it)
        }
        return result.val
    }

    /**
     * Blocks until all messages in the queue prior to call to await() complete.
     * Provides a means to synchronize with the Agent
     */
    final public void await() {
        sendAndWait {}
    }

    /**
     * Dynamically dispatches the method call
     */
    void handleMessage(final Object message) {
        onMessage message
    }

    /**
     * Adds a listener interested in state updates
     * A listener should be a closure accepting the old and the new value in this order.
     */
    public void addListener(Closure listener) {
        listeners.add checkClosure(listener)
    }

    /**
     * Adds a validator checking the agent's state
     * A validator should be a closure accepting the old and the new value in this order.
     */
    public void addValidator(Closure validator) {
        validators.add checkClosure(validator)
    }

    /**
     * Only two-argument closures are allowed
     * @param code The passed-in closure
     */
    @SuppressWarnings("GroovyMultipleReturnPointsPerMethod")
    private Closure checkClosure(Closure code) {
        if (!(code.maximumNumberOfParameters in [2, 3])) throw new IllegalArgumentException("Agent listeners and validators can only take two argments plus optionally the current agent instance as the first argument.")
        if (code.maximumNumberOfParameters == 3) return code.curry(this)
        else return code
    }

    /**
     * Creates an agent instance initialized with the given state.
     * The instance will use the default thread pool.
     * @param state The initial internal state of the new Agent instance
     * @return The created instance
     */
    public static final Agent agent(final def state) {
        Actors.defaultActorPGroup.agent(state)
    }

    /**
     * Creates an agent instance initialized with the given state.
     * The instance will use the default thread pool.
     * @param state The initial internal state of the new Agent instance
     * @param copy A closure to use to create a copy of the internal state when sending the internal state out
     * @return The created instance
     */
    public static final Agent agent(final def state, final Closure copy) {
        Actors.defaultActorPGroup.agent(state, copy)
    }

    /**
     * Creates an agent instance initialized with the given state, which will cooperate in thread sharing with other Agent instances
     * in a fair manner.
     * The instance will use the default thread pool.
     * @param state The initial internal state of the new Agent instance
     * @return The created instance
     */
    public static final Agent fairAgent(final def state) {
        Actors.defaultActorPGroup.fairAgent(state)
    }

    /**
     * Creates an agent instance initialized with the given state, which will cooperate in thread sharing with other Agent instances
     * in a fair manner.
     * The instance will use the default thread pool.
     * @param state The initial internal state of the new Agent instance
     * @param copy A closure to use to create a copy of the internal state when sending the internal state out
     * @return The created instance
     */
    public static final Agent fairAgent(final def state, final Closure copy) {
        Actors.defaultActorPGroup.fairAgent(state, copy)
    }
}
