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

package groovyx.gpars.dataflow.stream;

import groovy.lang.Closure;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.dataflow.DataFlowExpression;
import groovyx.gpars.dataflow.DataFlowReadChannel;
import groovyx.gpars.dataflow.DataFlowVariable;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Adapts a DataFlowStream to accommodate for the DataFlowReadChannel interface.
 * To minimize the overhead and stay in-line with the DataFlowStream semantics, the DataFlowStreamReadAdapter class is not thread-safe
 * and should only be used from within a single thread.
 * If multiple threads need to read from a DataFlowStream, they should each create their own wrapping DataFlowStreamReadAdapter.
 *
 * @param <T> The type of messages to pass through the stream
 */
public class DataFlowStreamReadAdapter<T> implements DataFlowReadChannel<T> {

    private DataFlowStream<T> head;
    private DataFlowStream<T> asyncHead;

    /**
     * Creates a new adapter
     *
     * @param stream The stream to wrap
     */
    public DataFlowStreamReadAdapter(final DataFlowStream<T> stream) {
        this.head = stream;
        this.asyncHead = head;
    }

    public Iterator<T> iterator() {
        return new FListIterator<T>(head);
    }

    @Override
    public String toString() {
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

    @Override
    public DataFlowExpression<T> poll() throws InterruptedException {
        final DataFlowVariable<T> firstDFV = head.getFirstDFV();
        if (firstDFV.isBound()) {
            moveHead();
            return firstDFV;
        } else return null;
    }

    private void moveHead() {
        if (head == asyncHead) moveAsyncHead();
        head = (DataFlowStream<T>) head.getRest();
    }

    private void moveAsyncHead() {
        asyncHead = (DataFlowStream<T>) asyncHead.getRest();
    }
}

