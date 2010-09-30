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
import groovyx.gpars.actor.impl.MessageStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Represents a thread-safe data flow stream. Values or DataFlowVariables are added using the '<<' operator
 * and safely read once available using the 'val' property.
 * The iterative methods like each(), collect(), iterator(), any(), all() or the for loops work with snapshots
 * of the stream at the time of calling the particular method.
 * For actors and Dataflow Operators the asynchronous non-blocking variants of the getValAsync() methods can be used.
 * They register the request to read a value and will send a message to the actor or operator once the value is available.
 *
 * @author Vaclav Pech
 *         Date: Jun 5, 2009
 */
@SuppressWarnings({"LawOfDemeter", "MethodReturnOfConcreteClass", "AnonymousInnerClass", "AnonymousInnerClassWithTooManyMethods"})
public final class DataFlowStream<T> implements DataFlowChannel<T> {

    /**
     * Internal lock
     */
    private final Object queueLock = new Object();

    /**
     * Stores the received DataFlowVariables in the buffer.
     */
    private final LinkedBlockingQueue<DataFlowVariable<T>> queue = new LinkedBlockingQueue<DataFlowVariable<T>>();

    /**
     * Stores unsatisfied requests for values
     */
    private final LinkedBlockingQueue<DataFlowVariable<T>> requests = new LinkedBlockingQueue<DataFlowVariable<T>>();

    /**
     * A collection of listeners who need to be informed each time the stream is bound to a value
     */
    private final Collection<MessageStream> wheneverBoundListeners = new CopyOnWriteArrayList<MessageStream>();

    /**
     * Adds a DataFlowVariable to the buffer.
     * Implementation detail - in fact another DFV is added to the buffer and an asynchronous 'whenBound' handler
     * is registered with the supplied DFV to update the one stored in the buffer.
     *
     * @param ref The DFV to add to the stream
     */
    @Override
    @SuppressWarnings("unchecked")
    public void leftShift(final DataFlowReadChannel<T> ref) {
        final DataFlowVariable<T> originalRef = retrieveForBind();
        hookWheneverBoundListeners(originalRef);

        ref.getValAsync(new MessageStream() {
            private static final long serialVersionUID = -4966523895011173569L;

            @Override
            public MessageStream send(final Object message) {
                originalRef.bind((T) message);
                return this;
            }
        });
    }

    /**
     * Adds a DataFlowVariable representing the passed in value to the buffer.
     *
     * @param value The value to bind to the head of the stream
     */
    @Override
    public void leftShift(final T value) {
        hookWheneverBoundListeners(retrieveForBind()).bind(value);
    }

    /**
     * Hooks the registered when bound handlers to the supplied dataflow expression
     *
     * @param expr The expression to hook all the when bound listeners to
     * @return The supplied expression handler to allow method chaining
     */
    private DataFlowExpression<T> hookWheneverBoundListeners(final DataFlowExpression<T> expr) {
        for (final MessageStream listener : wheneverBoundListeners) {
            expr.whenBound(listener);
        }
        return expr;
    }

    /**
     * Takes the first unsatisfied value request and binds a value on it.
     * If there are no unsatisfied value requests, a new DFV is stored in the queue.
     *
     * @return The DFV to bind the value on
     */
    private DataFlowVariable<T> retrieveForBind() {
        return copyDFV(requests, queue);
    }

    private DataFlowVariable<T> copyDFV(final LinkedBlockingQueue<DataFlowVariable<T>> from, final Queue<DataFlowVariable<T>> to) {
        DataFlowVariable<T> ref;
        synchronized (queueLock) {
            ref = from.poll();
            if (ref == null) {
                ref = new DataFlowVariable<T>();
                to.offer(ref);
            }
        }
        return ref;
    }

    /**
     * Retrieves the value at the head of the buffer. Blocks until a value is available.
     *
     * @return The value bound to the DFV at the head of the stream
     * @throws InterruptedException If the current thread is interrupted
     */
    @Override
    public T getVal() throws InterruptedException {
        return retrieveOrCreateVariable().getVal();
    }

    /**
     * Retrieves the value at the head of the buffer. Blocks until a value is available.
     *
     * @param timeout The timeout value
     * @param units   Units for the timeout
     * @return The value bound to the DFV at the head of the stream
     * @throws InterruptedException If the current thread is interrupted
     */
    @Override
    public T getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        return retrieveOrCreateVariable().getVal(timeout, units);
    }

    /**
     * Retrieves the value at the head of the buffer. Returns null, if no value is available.
     *
     * @return The value bound to the DFV at the head of the stream or null
     * @throws InterruptedException If the current thread is interrupted
     */
    @Override
    public T poll() throws InterruptedException {
        synchronized (queueLock) {
            final DataFlowVariable<T> df = queue.peek();
            if (df != null && df.isBound()) {
                queue.poll();
                return df.getVal();
            }
            return null;
        }
    }

    /**
     * Asynchronously retrieves the value at the head of the buffer. Sends the actual value of the variable as a message
     * back the the supplied actor once the value has been bound.
     * The actor can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow Variable.
     *
     * @param callback The actor to notify when a value is bound
     */
    @Override
    public void getValAsync(final MessageStream callback) {
        getValAsync(null, callback);
    }

    /**
     * Asynchronously retrieves the value at the head of the buffer. Sends a message back the the supplied actor / operator
     * with a map holding the supplied index under the 'index' key and the actual value of the variable under
     * the 'result' key once the value has been bound.
     * The actor/operator can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow Variable.
     *
     * @param attachment An arbitrary value to identify operator channels and so match requests and replies
     * @param callback   The actor / operator to notify when a value is bound
     */
    @Override
    public void getValAsync(final Object attachment, final MessageStream callback) {
        retrieveOrCreateVariable().getValAsync(attachment, callback);
    }

    /**
     * Schedule closure to be executed by pooled actor after data became available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled
     *
     * @param closure closure to execute when data available
     */
    @Override
    public void rightShift(final Closure closure) {
        whenBound(closure);
    }

    /**
     * Schedule closure to be executed by pooled actor after the next data becomes available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param closure closure to execute when data available
     */
    @Override
    public void whenBound(final Closure closure) {
        getValAsync(new DataCallback(closure, DataFlowExpression.retrieveCurrentDFPGroup()));
    }

    /**
     * Send the next bound piece of data to the provided stream when it becomes available
     *
     * @param stream stream where to send result
     */
    @Override
    public void whenBound(final MessageStream stream) {
        getValAsync(stream);
    }

    /**
     * Send all pieces of data bound in the future to the provided stream when it becomes available     *
     *
     * @param closure closure to execute when data available
     */
    @Override
    public void wheneverBound(final Closure closure) {
        wheneverBoundListeners.add(new DataCallback(closure, DataFlowExpression.retrieveCurrentDFPGroup()));
    }

    /**
     * Send all pieces of data bound in the future to the provided stream when it becomes available
     *
     * @param stream stream where to send result
     */
    @Override
    public void wheneverBound(final MessageStream stream) {
        wheneverBoundListeners.add(stream);
    }

    /**
     * Check if value has been set already for this expression
     *
     * @return true if bound already
     */
    @Override
    public boolean isBound() {
        return !queue.isEmpty();
    }

    /**
     * Checks whether there's a DFV waiting in the queue and retrieves it. If not, a new unmatched value request, represented
     * by a new DFV, is added to the requests queue.
     *
     * @return The DFV to wait for value on
     */
    private DataFlowVariable<T> retrieveOrCreateVariable() {
        return copyDFV(queue, requests);
    }

    /**
     * Returns the current size of the buffer
     *
     * @return Number of DFVs in the queue
     */
    public int length() {
        return queue.size();
    }

    /**
     * Returns an iterator over a current snapshot of the buffer's content. The next() method returns actual values
     * not the DataFlowVariables.
     *
     * @return AN iterator over all DFVs in the queue
     */
    public Iterator<T> iterator() {
        final Iterator<DataFlowVariable<T>> iterator = queue.iterator();
        return new Iterator<T>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                try {
                    return iterator.next().getVal();
                } catch (InterruptedException e) {
                    throw new IllegalStateException("The thread has been interrupted, which prevented the iterator from retrieving the next element.", e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove not available");
            }
        };

    }

    @Override
    public String toString() {
        return "DataFlowStream(queue=" + new ArrayList<DataFlowVariable<T>>(queue).toString() + ')';
    }
}
