// GPars (formerly GParallelizer)
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

import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.util.EnhancedRWLock
import org.codehaus.groovy.runtime.NullObject

/**
 * @author Vaclav Pech
 * Date: 13.4.2010
 */
public class Agent<T> extends AgentCore {
    //todo exception handlers
    //todo grouping actors and thread pools
    //todo javadoc
    //todo tutorial
    //todo reconsider Safe

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
     * Creates a new Safe with the internal state set to null
     */
    def Agent() {
        this(null)
    }

    /**
     * Creates a new Safe around the supplied modifiable object
     * @param data The object to use for storing the internal state of the variable
     */
    def Agent(final T data) {
        this.data = data
    }

    /**
     * Creates a new Safe around the supplied modifiable object
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
            code.call(data)
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
    final void updateValue(T newValue) { data = newValue }

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
     * Provides a means to synchronize with the Safe
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
}