package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.Actor

/**
 * A special-purpose thread-safe non-blocking reference implementation inspired by Agents in Clojure.
 * A SafeVariable wraps a reference to mutable state, held inside a single field, and accepts code (closures)
 * as messages, which can be sent to the SafeVariable just like to any other actor using the '<<' operator
 * or any of the send() methods.
 * After reception of a closure, the closure is invoked on the internal mutable field. The closure is guaranteed
 * to be run without intervention from other threads and so may freely alter the state.
 * The return value of the closure is sent in reply to the sender of the closure.
 * If the message sent to a SafeVariable is not a closure, it is considered to be a new value for the internal
 * reference field. The internal reference can also be changes using the updateValue() method from within the received
 * closures.
 * The 'val' property will sefely return the current value of the SafeVariable, while the valAsync() method
 * will do the same without blocking the caller.
 * The initial internal value can be passed to the constructor. The two-parameter constructor allows to alter the way
 * the internal value is returned from val/valAsync. By default the original reference is returned, but in many scenarios a copy
 * or a clone might be more appropriate.
 *
 * @author Vaclav Pech
 * Date: Jul 2, 2009
 * Time: 12:43:59 PM
 * To change this template use File | Settings | File Templates.
 */
public final class SafeVariable<T> extends DynamicDispatchActor {

    /**
     * Holds the internal mutable state
     */
    private T data
    private Closure copy = {it}

    /**
     * Creates a new Agent around the supplied modifiable object
     */
    def SafeVariable() {
        this.data = null
    }

    /**
     * Creates a new Agent around the supplied modifiable object
     */
    def SafeVariable(final T data) {
        this.data = data
    }

    /**
     * Creates a new Agent around the supplied modifiable object
     */
    def SafeVariable(final T data, final Closure copy) {
        this.data = data
        this.copy = copy
    }

    //todo test with data being null
    //todo test with null as a valid return value from the closure
    //todo test reply() inside the closure
    //todo various copy strategies
    /**
     * Accepts and invokes the closure
     */
    void onMessage(Closure code) {
        code.delegate = this
        replyIfExists code.call(data)
    }

    /**
     * Other messages than closures are not supported
     */
    void onMessage(T message) { data = message }

    void updateValue(T newValue) { data = newValue }

    /**
     * A shorthand method for safe message-based retrieval of the internal state.
     */
    public T getVal() {
        this.sendAndWait {copy(it)}
    }

    //todo test, document
    public void valAsync(Closure callback) {
        PooledActors.actor {
            callback.call(this.getVal())
        }.start()
    }
}