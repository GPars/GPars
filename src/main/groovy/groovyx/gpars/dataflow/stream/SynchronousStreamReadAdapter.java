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

package groovyx.gpars.dataflow.stream;

import groovy.lang.Closure;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.expression.DataflowExpression;
import groovyx.gpars.group.PGroup;
import groovyx.gpars.scheduler.Pool;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;

/**
 * Adapts a DataflowStream to accommodate for the DataflowReadChannel interface.
 * To minimize the overhead and stay in-line with the DataflowStream semantics, the DataflowStreamReadAdapter class is not thread-safe
 * and should only be used from within a single thread.
 * If multiple threads need to read from a DataflowStream, they should each create their own wrapping DataflowStreamReadAdapter.
 *
 * @param <T> The type of messages to pass through the stream
 * @author Vaclav Pech
 */
public final class SynchronousStreamReadAdapter<T> implements DataflowReadChannel<T> {

    private static final String ERROR_READING_FROM_THE_SYNCHRONOUS_CHANNEL = "Error reading from the synchronous channel";
    private StreamCore<SynchronousValue<T>> head;
    private StreamCore<SynchronousValue<T>> asyncHead;
    private boolean disabled = false;

    /**
     * Creates a new adapter
     *
     * @param stream The stream to wrap
     */
    public SynchronousStreamReadAdapter(final StreamCore<SynchronousValue<T>> stream) {
        disabled = true;
        this.head = stream;
        this.asyncHead = head;
    }

    /**
     * Called from SynchronousBroadcast.deregisterReadChannel() to invalidate the adapter
     * Subsequent reads from the adapter will not succeed.
     */
    public void deregister() {
        head = null;
        asyncHead = null;
    }

    public Iterator<SynchronousValue<T>> iterator() {
        checkDisabled();
        return new FListIterator<SynchronousValue<T>>(head);
    }

    private void checkDisabled() {
        if (disabled)
            throw new IllegalStateException("The subscription has been de-registered from the broadcast already and cannot be used.");
    }

    @Override
    public String toString() {
        checkDisabled();
        return head.toString();
    }

    @Override
    public T getVal() throws InterruptedException {
        checkDisabled();
        final SynchronousValue<T> synchronousValue = head.getFirst();
        final T first = synchronousValue.getValue();
        moveHead();
        awaitParties(synchronousValue);
        return first;
    }

    @Override
    public T getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        checkDisabled();
        head.getFirstDFV().getVal(timeout, units);
        if (head.getFirstDFV().isBound()) {
            final SynchronousValue<T> synchronousValue = head.getFirst();
            moveHead();
            awaitParties(synchronousValue);
            return synchronousValue.getValue();
        } else {
            return null;
        }
    }

    @Override
    public void getValAsync(final MessageStream callback) {
        checkDisabled();
        asyncHead.getFirstDFV().getValAsync(new MessageStream() {
            @Override
            public MessageStream send(final Object message) {
                final SynchronousValue<T> synchronousValue = (SynchronousValue<T>) message;
                awaitParties(synchronousValue);
                callback.send(synchronousValue.getValue());
                return null;
            }
        });
        moveAsyncHead();
    }

    @Override
    public void getValAsync(final Object attachment, final MessageStream callback) {
        checkDisabled();
        asyncHead.getFirstDFV().getValAsync(attachment, new MessageStream() {
            @Override
            public MessageStream send(final Object message) {
                final Map<String, Object> typedMessage = (Map<String, Object>) message;
                final Object messageValue = typedMessage.get("result");
                final SynchronousValue<T> synchronousValue = (SynchronousValue<T>) messageValue;
                awaitParties(synchronousValue);
                typedMessage.put("result", synchronousValue.getValue());
                callback.send(typedMessage);
                return null;
            }
        });
        moveAsyncHead();
    }

    @Override
    public void rightShift(final Closure closure) {
        whenBound(closure);
    }

    @Override
    public void whenBound(final Closure closure) {
        checkDisabled();
        //todo handle
        asyncHead.getFirstDFV().whenBound(closure);
        moveAsyncHead();
    }

    /**
     * Schedule closure to be executed by pooled actor after data becomes available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param pool    The thread pool to use for task scheduling for asynchronous message delivery
     * @param closure closure to execute when data available
     */
    @Override
    public void whenBound(final Pool pool, final Closure closure) {
        checkDisabled();
        //todo handle
        asyncHead.getFirstDFV().whenBound(pool, closure);
        moveAsyncHead();
    }

    @Override
    public void whenBound(final PGroup group, final Closure closure) {
        checkDisabled();
        //todo handle
        asyncHead.getFirstDFV().whenBound(group, closure);
        moveAsyncHead();
    }

    @Override
    public void whenBound(final MessageStream stream) {
        checkDisabled();
        //todo handle
        asyncHead.getFirstDFV().whenBound(stream);
        moveAsyncHead();
    }

    @Override
    public void wheneverBound(final Closure closure) {
        checkDisabled();
        //todo handle
        head.wheneverBound(closure);
    }

    @Override
    public void wheneverBound(final MessageStream stream) {
        checkDisabled();
        //todo handle
        head.wheneverBound(stream);
    }

    @Override
    public boolean isBound() {
        checkDisabled();
        return head.getFirstDFV().isBound();
    }

    @Override
    public DataflowExpression<T> poll() throws InterruptedException {
        checkDisabled();
        final DataflowVariable<SynchronousValue<T>> firstDFV = head.getFirstDFV();
        if (firstDFV.isBound()) {
            moveHead();
            return null;  //firstDFV;
        } else return null;
    }

    private void moveHead() {
        if (head == asyncHead) moveAsyncHead();
        head = (StreamCore<SynchronousValue<T>>) head.getRest();
    }

    private void moveAsyncHead() {
        asyncHead = (StreamCore<SynchronousValue<T>>) asyncHead.getRest();
    }

    private void awaitParties(final SynchronousValue<T> synchronousValue) {
        try {
            synchronousValue.getBarrier().await();
        } catch (BrokenBarrierException e) {
            throw new RuntimeException(ERROR_READING_FROM_THE_SYNCHRONOUS_CHANNEL, e);
        } catch (InterruptedException e) {
            throw new RuntimeException(ERROR_READING_FROM_THE_SYNCHRONOUS_CHANNEL, e);
        }
    }
}

