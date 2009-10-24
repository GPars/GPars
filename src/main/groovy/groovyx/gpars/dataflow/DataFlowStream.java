//  GPars (formerly GParallelizer)
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

package groovyx.gpars.dataflow;

import groovyx.gpars.MessageStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

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
public final class DataFlowStream<T> {

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
     * Adds a DataFlowVariable to the buffer.
     * Implementation detail - in fact another DFV is added to the buffer and an asynchronous 'whenBound' handler
     * is registered with the supplied DFV to update the one stired in the buffer.
     *
     * @param ref The DFV to add to the stream
     */
    @SuppressWarnings("unchecked")
    public void leftShift(final DataFlowExpression<T> ref) {
        final DataFlowVariable<T> originalRef = retrieveForBind();
        ref.getValAsync(new MessageStream() {
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
    public void leftShift(final T value) {
        retrieveForBind().bind(value);
    }

    /**
     * Takes the first unsatisfied value request and binds a value on it.
     * If there are no unsatisfies value requests, a new DFV is stored in the queue.
     *
     * @return The DFV to bind the value on
     */
    private DataFlowVariable<T> retrieveForBind() {
        DataFlowVariable<T> ref;
        synchronized (queueLock) {
            ref = requests.poll();
            if (ref == null) {
                ref = new DataFlowVariable<T>();
                queue.offer(ref);
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
    public T getVal() throws InterruptedException {
        return retrieveOrCreateVariable().getVal();
    }

    /**
     * Asynchronously retrieves the value at the head of the buffer. Sends the actual value of the variable as a message
     * back the the supplied actor once the value has been bound.
     * The actor can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow Variable.
     *
     * @param messageStream The actor to notify when a value is bound
     */
    public void getValAsync(final MessageStream messageStream) {
        getValAsync(null, messageStream);
    }

    /**
     * Asynchronously retrieves the value at the head of the buffer. Sends a message back the the supplied actor / operator
     * with a map holding the supplied index under the 'index' key and the actual value of the variable under
     * the 'result' key once the value has been bound.
     * The actor/operator can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow Variable.
     *
     * @param attachment    An arbitrary value to identify operator channels and so match requests and replies
     * @param messageStream The actor / operator to notify when a value is bound
     */
    public void getValAsync(final Object attachment, final MessageStream messageStream) {
        retrieveOrCreateVariable().getValAsync(attachment, messageStream);
    }

    /**
     * Checks whether there's a DFV waiting in the queue and retrieves it. If not, a new unmatch value request, represented
     * by a new DFV, is added to the requests queue.
     *
     * @return The DFV to wait for value on
     */
    private DataFlowVariable<T> retrieveOrCreateVariable() {
        DataFlowVariable<T> dataFlowVariable;
        synchronized (queueLock) {
            dataFlowVariable = queue.poll();
            if (dataFlowVariable == null) {
                dataFlowVariable = new DataFlowVariable<T>();
                requests.offer(dataFlowVariable);
            }
        }
        return dataFlowVariable;
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

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public T next() {
                try {
                    return iterator.next().getVal();
                } catch (InterruptedException e) {
                    throw new IllegalStateException("The thread has been interrupted, which prevented the iterator from retrieving the next element.", e);
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("Remove not available");
            }
        };

    }

    @Override public String toString() {
        return "DataFlowStream(queue=" + new ArrayList<DataFlowVariable<T>>(queue).toString() + ')';
    }
}
