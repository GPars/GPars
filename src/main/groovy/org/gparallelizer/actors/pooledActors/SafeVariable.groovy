//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.pooledActors.DynamicDispatchActor
import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.util.EnhancedRWLock

/**
 * A special-purpose thread-safe non-blocking reference implementation inspired by Agents in Clojure.
 * Agents safe-guard mutable values by allowing only a single agent-managed thread to make modifications to them.
 * The mutable values are not directly accessible from outside, but instead requests have to be sent to the agent
 * and the agent guarantees to process the requests sequentially on behalf of the callers. Agents guarantee sequential
 * execution of all requests and so consistency of the values. 
 * A SafeVariable wraps a reference to mutable state, held inside a single field, and accepts code (closures / commands)
 * as messages, which can be sent to the SafeVariable just like to any other actor using the '<<' operator
 * or any of the send() methods.
 * After reception of a closure / command, the closure is invoked against the internal mutable field. The closure is guaranteed
 * to be run without intervention from other threads and so may freely alter the internal state of the SafeVariable
 * held in the internal <i>data</i> field.
 * The return value of the submitted closure is sent in reply to the sender of the closure.
 * If the message sent to a SafeVariable is not a closure, it is considered to be a new value for the internal
 * reference field. The internal reference can also be changed using the updateValue() method from within the received
 * closures.
 * The 'val' property of a SafeVariable will safely return the current value of the SafeVariable, while the valAsync() method
 * will do the same without blocking the caller.
 * The 'instantVal' property will retrieve the current value of the SafeVariable without having to wait in the queue of tasks.
 * The initial internal value can be passed to the constructor. The two-parameter constructor allows to alter the way
 * the internal value is returned from val/valAsync. By default the original reference is returned, but in many scenarios a copy
 * or a clone might be more appropriate.
 *
 * @author Vaclav Pech
 * Date: Jul 2, 2009
 */
public class SafeVariable extends DynamicDispatchActor {

    //todo generics were not accepted by Gradle :compile
    
    private EnhancedRWLock lock = new EnhancedRWLock()
    /**
     * Holds the internal mutable state
     */
    protected Object data
    final Closure copy = {it}

    /**
     * Creates a new SafeVariable with the internal state set to null
     */
    def SafeVariable() {
        this(null)
    }

    /**
     * Creates a new SafeVariable around the supplied modifiable object
     * @param data The object to use for storing the variable's internal state     *
     */
    def SafeVariable(final Object data) {
        this.data = data
        start()
    }

    /**
     * Creates a new SafeVariable around the supplied modifiable object
     * @param data The object to use for storing the variable's internal state
     * @param copy A closure to use to create a copy of the internal state when sending the internal state out
     */
    def SafeVariable(final Object data, final Closure copy) {
        this.data = data
        this.copy = copy
        start()
    }

    /**
     * Accepts and invokes the closure
     */
    final void onMessage(Closure code) {
        lock.withWriteLock {
            code.delegate = this
            replyIfExists code.call(data)
        }
    }

    /**
     * Other messages than closures are accepted as new values for the internal state
     */
    final void onMessage(Object message) {
        lock.withWriteLock {
            updateValue message
        }
    }

    /**
     * Allows closures to set the new internal state as a whole
     */
    final void updateValue(Object newValue) { data = newValue }

    /**
     * A shorthand method for safe message-based retrieval of the internal state.
     * Retrieves the internal state immediately by-passing the queue of tasks waiting to be processed.
     */
    final public Object getInstantVal() {
        Object result = null
        lock.withReadLock { result = copy(data) }
        return result
    }

    /**
     * A shorthand method for safe message-based retrieval of the internal state.
     */
    final public Object getVal() {
        this.sendAndWait { getInstantVal() }
    }

    /**
     * A shorthand method for safe asynchronous message-based retrieval of the internal state.
     * @param callback A closure to invoke with the internal state as a parameter
     */
    final public void valAsync(Closure callback) {
        PooledActors.actor {
            callback.call(this.getVal())
        }.start()
    }

    //todo javadoc
    final public void await() {
        this.sendAndWait {}
    }
}
