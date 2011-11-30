// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.dataflow;

import groovy.lang.Closure;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.dataflow.expression.DataflowExpression;
import groovyx.gpars.group.PGroup;
import groovyx.gpars.scheduler.Pool;

import java.util.concurrent.TimeUnit;

/**
 * A common interface for all dataflow variables, streams or queues
 *
 * @author Vaclav Pech
 *         Date: 21st Sep 2010
 */
public interface DataflowReadChannel<T> {

    /**
     * Asynchronously retrieves the value from the channel. Sends the actual value of the channel as a message
     * back the the supplied actor once the value has been bound.
     * The actor can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow channel.
     *
     * @param callback An actor to send the bound value to.
     */
    void getValAsync(final MessageStream callback);

    /**
     * Asynchronously retrieves the value from the channel. Sends a message back the the supplied MessageStream
     * with a map holding the supplied attachment under the 'attachment' key and the actual value of the channel under
     * the 'result' key once the value has been bound.
     * Attachment is an arbitrary value helping the actor.operator match its request with the reply.
     * The actor/operator can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow channel.
     *
     * @param attachment arbitrary non-null attachment if reader needs better identification of result
     * @param callback   An actor to send the bound value plus the supplied index to.
     */
    void getValAsync(final Object attachment, final MessageStream callback);

    /**
     * Reads the current value of the channel. Blocks, if the value has not been assigned yet.
     *
     * @return The actual value
     * @throws InterruptedException If the current thread gets interrupted while waiting for the channel to be bound
     */
    T getVal() throws InterruptedException;

    /**
     * Reads the current value of the channel. Blocks up to given timeout, if the value has not been assigned yet.
     *
     * @param timeout The timeout value
     * @param units   Units for the timeout
     * @return The actual value
     * @throws InterruptedException If the current thread gets interrupted while waiting for the channel to be bound
     */
    T getVal(final long timeout, final TimeUnit units) throws InterruptedException;

    /**
     * Schedule closure to be executed after data became available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled
     *
     * @param closure closure to execute when data available
     * @return A promise for the results of the supplied closure. This allows for chaining of then() method calls.
     */
    <V> Promise<V> rightShift(final Closure closure);

    /**
     * Schedule closure to be executed after data becomes available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param closure closure to execute when data available
     */
    void whenBound(final Closure closure);

    /**
     * Schedule closure to be executed after data becomes available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param pool    The thread pool to use for task scheduling for asynchronous message delivery
     * @param closure closure to execute when data available
     */
    void whenBound(final Pool pool, final Closure closure);

    /**
     * Schedule closure to be executed after data becomes available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param group   The PGroup to use for task scheduling for asynchronous message delivery
     * @param closure closure to execute when data available
     */
    void whenBound(final PGroup group, final Closure closure);

    /**
     * Send the bound data to provided stream when it becomes available
     *
     * @param stream stream where to send result
     */
    void whenBound(final MessageStream stream);

    /**
     * Schedule closure to be executed after data became available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled
     *
     * @param closure closure to execute when data available
     * @return A promise for the results of the supplied closure. This allows for chaining of then() method calls.
     */
    <V> Promise<V> then(final Closure closure);

    /**
     * Schedule closure to be executed after data becomes available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param pool    The thread pool to use for task scheduling for asynchronous message delivery
     * @param closure closure to execute when data available
     * @return A promise for the results of the supplied closure. This allows for chaining of then() method calls.
     */
    <V> Promise<V> then(final Pool pool, final Closure closure);

    /**
     * Schedule closure to be executed after data becomes available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param group   The PGroup to use for task scheduling for asynchronous message delivery
     * @param closure closure to execute when data available
     * @return A promise for the results of the supplied closure. This allows for chaining of then() method calls.
     */
    <V> Promise<V> then(final PGroup group, final Closure closure);

    /**
     * Send all pieces of data bound in the future to the provided stream when it becomes available.     *
     *
     * @param closure closure to execute when data available
     */
    void wheneverBound(final Closure closure);

    /**
     * Send all pieces of data bound in the future to the provided stream when it becomes available.
     *
     * @param stream stream where to send result
     */
    void wheneverBound(final MessageStream stream);

    /**
     * Creates and attaches a new operator processing values from the channel
     *
     * @param closure The function to invoke on all incoming values as part of the new operator's body
     * @param <V>     The type of values returned from the supplied closure
     * @return A channel of the same type as this channel, which the new operator will output into.
     */
    <V> DataflowReadChannel<V> chainWith(final Closure<V> closure);

    /**
     * Creates and attaches a new operator processing values from the channel
     *
     * @param pool    The thread pool to use for task scheduling for asynchronous message delivery
     * @param closure The function to invoke on all incoming values as part of the new operator's body
     * @param <V>     The type of values returned from the supplied closure
     * @return A channel of the same type as this channel, which the new operator will output into.
     */
    <V> DataflowReadChannel<V> chainWith(final Pool pool, final Closure<V> closure);

    /**
     * Creates and attaches a new operator processing values from the channel
     *
     * @param group   The PGroup to use for task scheduling for asynchronous message delivery
     * @param closure The function to invoke on all incoming values as part of the new operator's body
     * @param <V>     The type of values returned from the supplied closure
     * @return A channel of the same type as this channel, which the new operator will output into.
     */
    <V> DataflowReadChannel<V> chainWith(final PGroup group, final Closure<V> closure);

    /**
     * Check if value has been set already for this expression
     *
     * @return true if bound already
     */
    boolean isBound();

    /**
     * Reports the current number of elements in the channel
     *
     * @return The current snapshot of the number of elements in the channel
     */
    int length();

    /**
     * Retrieves the value at the head of the buffer. Returns null, if no value is available.
     *
     * @return The value bound to the DFV at the head of the stream or null
     * @throws InterruptedException If the current thread is interrupted
     */
    @SuppressWarnings({"ClassReferencesSubclass"})
    DataflowExpression<T> poll() throws InterruptedException;
}
