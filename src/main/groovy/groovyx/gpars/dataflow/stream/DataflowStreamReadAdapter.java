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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
public class DataflowStreamReadAdapter<T> implements DataflowReadChannel<T> {

    private StreamCore<T> head;
    private StreamCore<T> asyncHead;

    /**
     * Creates a new adapter
     *
     * @param stream The stream to wrap
     */
    public DataflowStreamReadAdapter(final StreamCore<T> stream) {
        this.head = stream;
        this.asyncHead = head;
    }

    public Iterator<T> iterator() {
        return new FListIterator<T>(head);
    }

    @Override
    public final String toString() {
        return head.toString();
    }

    @Override
    public T getVal() throws InterruptedException {
        final T first = head.getFirst();
        moveHead();
        return first;
    }

    @Override
    public T getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        head.getFirstDFV().getVal(timeout, units);
        if (head.getFirstDFV().isBound()) {
            final T result = head.getFirst();
            moveHead();
            return result;
        } else {
            return null;
        }
    }

    @Override
    public void getValAsync(final MessageStream callback) {
        asyncHead.getFirstDFV().getValAsync(callback);
        moveAsyncHead();
    }

    @Override
    public void getValAsync(final Object attachment, final MessageStream callback) {
        asyncHead.getFirstDFV().getValAsync(attachment, callback);
        moveAsyncHead();
    }

    @Override
    public void rightShift(final Closure closure) {
        whenBound(closure);
    }

    @Override
    public void whenBound(final Closure closure) {
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
        asyncHead.getFirstDFV().whenBound(pool, closure);
        moveAsyncHead();
    }

    @Override
    public void whenBound(final PGroup group, final Closure closure) {
        asyncHead.getFirstDFV().whenBound(group, closure);
        moveAsyncHead();
    }

    @Override
    public void whenBound(final MessageStream stream) {
        asyncHead.getFirstDFV().whenBound(stream);
        moveAsyncHead();
    }

    @Override
    public void wheneverBound(final Closure closure) {
        head.wheneverBound(closure);
    }

    @Override
    public void wheneverBound(final MessageStream stream) {
        head.wheneverBound(stream);
    }

    @Override
    public boolean isBound() {
        return head.getFirstDFV().isBound();
    }

    /**
     * Returns the current size of the buffer
     *
     * @return Number of DFVs in the queue
     */
    @Override
    public int length() {
        StreamCore<T> current = head;
        int length = 0;
        while (current.getFirstDFV().isBound()) {
            length += 1;
            current = (StreamCore<T>) current.getRest();
        }
        return length;
    }

    @Override
    public DataflowExpression<T> poll() throws InterruptedException {
        final DataflowVariable<T> firstDFV = head.getFirstDFV();
        if (firstDFV.isBound()) {
            moveHead();
            return firstDFV;
        } else return null;
    }

    protected final List<DataflowVariable<T>> allUnprocessedDFVs() throws InterruptedException {
        final List<DataflowVariable<T>> values = new ArrayList<DataflowVariable<T>>();
        StreamCore<T> currentHead = asyncHead;
        while (currentHead != null) {
            values.add(currentHead.getFirstDFV());
            currentHead = (StreamCore<T>) currentHead.rest.get();
        }
        return values;
    }

    private void moveHead() {
        if (head == asyncHead) moveAsyncHead();
        head = (StreamCore<T>) head.getRest();
    }

    private void moveAsyncHead() {
        asyncHead = (StreamCore<T>) asyncHead.getRest();
    }
}

