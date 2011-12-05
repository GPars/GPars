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
import groovyx.gpars.dataflow.Dataflow;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.DataflowWriteChannel;
import groovyx.gpars.dataflow.Promise;
import groovyx.gpars.dataflow.SyncDataflowVariable;
import groovyx.gpars.dataflow.expression.DataflowExpression;
import groovyx.gpars.dataflow.impl.ThenMessagingRunnable;
import groovyx.gpars.dataflow.operator.ChainWithClosure;
import groovyx.gpars.dataflow.operator.CopyChannelsClosure;
import groovyx.gpars.dataflow.operator.FilterClosure;
import groovyx.gpars.group.DefaultPGroup;
import groovyx.gpars.group.PGroup;
import groovyx.gpars.scheduler.Pool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

/**
 * Adapts a DataflowStream to accommodate for the DataflowReadChannel interface.
 * To minimize the overhead and stay in-line with the DataflowStream semantics, the DataflowStreamReadAdapter class is not thread-safe
 * and should only be used from within a single thread.
 * If multiple threads need to read from a DataflowStream, they should each create their own wrapping DataflowStreamReadAdapter.
 *
 * @param <T> The type of messages to pass through the stream
 * @author Vaclav Pech
 */
@SuppressWarnings({"unchecked"})
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
        final T value = head.getFirstDFV().getVal(timeout, units);
        if (value == null) {
            if (shouldReportTimeout()) {
                return null;
            } else {
                final T result = head.getFirstDFV().getVal();
                moveHead();
                return result;
            }
        } else {
            moveHead();
            return value;
        }
    }

    private boolean shouldReportTimeout() {
        final DataflowVariable<T> firstDFV = head.getFirstDFV();
        if (!firstDFV.isBound()) return true;
        if (firstDFV instanceof SyncDataflowVariable) {
            return ((SyncDataflowVariable) firstDFV).awaitingParties();
        }
        return false;
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
    public <V> Promise<V> rightShift(final Closure closure) {
        return then(closure);
    }

    @Override
    public void whenBound(final Closure closure) {
        asyncHead.getFirstDFV().whenBound(closure);
        moveAsyncHead();
    }

    /**
     * Schedule closure to be executed by pooled actor after data becomes available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param pool    The thread pool to use for task scheduling for asynchronous message delivery
     * @param closure closure to execute when data becomes available. The closure should take at most one argument.
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

    /**
     * Schedule closure to be executed after data became available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled
     *
     * @param closure closure to execute when data becomes available. The closure should take at most one argument.
     * @return A promise for the results of the supplied closure. This allows for chaining of then() method calls.
     */
    @Override
    public final <V> Promise<V> then(final Closure closure) {
        final DataflowVariable<V> result = new DataflowVariable<V>();
        whenBound(new ThenMessagingRunnable<T, V>(result, closure));
        return result;
    }

    /**
     * Schedule closure to be executed after data becomes available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param pool    The thread pool to use for task scheduling for asynchronous message delivery
     * @param closure closure to execute when data becomes available. The closure should take at most one argument.
     * @return A promise for the results of the supplied closure. This allows for chaining of then() method calls.
     */
    @Override
    public <V> Promise<V> then(final Pool pool, final Closure closure) {
        final DataflowVariable<V> result = new DataflowVariable<V>();
        whenBound(pool, new ThenMessagingRunnable<T, V>(result, closure));
        return result;
    }

    /**
     * Schedule closure to be executed after data becomes available.
     * It is important to notice that even if the expression is already bound the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param group   The PGroup to use for task scheduling for asynchronous message delivery
     * @param closure closure to execute when data becomes available. The closure should take at most one argument.
     * @return A promise for the results of the supplied closure. This allows for chaining of then() method calls.
     */
    @Override
    public <V> Promise<V> then(final PGroup group, final Closure closure) {
        final DataflowVariable<V> result = new DataflowVariable<V>();
        whenBound(group, new ThenMessagingRunnable<T, V>(result, closure));
        return result;
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
    public final <V> DataflowReadChannel<V> chainWith(final Closure<V> closure) {
        return chainWith(Dataflow.DATA_FLOW_GROUP, closure);
    }

    @Override
    public final <V> DataflowReadChannel<V> chainWith(final Pool pool, final Closure<V> closure) {
        return chainWith(new DefaultPGroup(pool), closure);
    }

    @Override
    public <V> DataflowReadChannel<V> chainWith(final PGroup group, final Closure<V> closure) {
        final DataflowQueue<V> result = new DataflowQueue<V>();
        group.operator(this, result, new ChainWithClosure<V>(closure));
        return result;
    }

    @Override
    public <V> DataflowReadChannel<V> or(final Closure<V> closure) {
        return chainWith(closure);
    }

    @Override
    public <V> DataflowReadChannel<V> filter(final Closure<Boolean> closure) {
        return chainWith(new FilterClosure(closure));
    }

    @Override
    public <V> DataflowReadChannel<V> filter(final Pool pool, final Closure<Boolean> closure) {
        return chainWith(pool, new FilterClosure(closure));
    }

    @Override
    public <V> DataflowReadChannel<V> filter(final PGroup group, final Closure<Boolean> closure) {
        return chainWith(group, new FilterClosure(closure));
    }

    @Override
    public <V> void into(final DataflowWriteChannel<V> target) {
        into(Dataflow.DATA_FLOW_GROUP, target);
    }

    @Override
    public <V> void into(final Pool pool, final DataflowWriteChannel<V> target) {
        into(new DefaultPGroup(pool), target);
    }

    @Override
    public <V> void into(final PGroup group, final DataflowWriteChannel<V> target) {
        group.operator(this, target, new ChainWithClosure(new CopyChannelsClosure()));
    }

    @Override
    public <V> void or(final DataflowWriteChannel<V> target) {
        into(target);
    }

    @Override
    public <V> void split(final DataflowWriteChannel<V> target1, final DataflowWriteChannel<V> target2) {
        split(Dataflow.DATA_FLOW_GROUP, target1, target2);
    }

    @Override
    public <V> void split(final Pool pool, final DataflowWriteChannel<V> target1, final DataflowWriteChannel<V> target2) {
        split(new DefaultPGroup(pool), target1, target2);
    }

    @Override
    public <V> void split(final PGroup group, final DataflowWriteChannel<V> target1, final DataflowWriteChannel<V> target2) {
        split(group, asList(target1, target2));
    }

    @Override
    public <V> void split(final List<DataflowWriteChannel<V>> targets) {
        split(Dataflow.DATA_FLOW_GROUP, targets);
    }

    @Override
    public <V> void split(final Pool pool, final List<DataflowWriteChannel<V>> targets) {
        split(new DefaultPGroup(pool), targets);
    }

    @Override
    public <V> void split(final PGroup group, final List<DataflowWriteChannel<V>> targets) {
        group.operator(asList(this), targets, new ChainWithClosure(new CopyChannelsClosure()));
    }

    @Override
    public <V> DataflowReadChannel<V> tap(final DataflowWriteChannel<V> target) {
        return tap(Dataflow.DATA_FLOW_GROUP, target);
    }

    @Override
    public <V> DataflowReadChannel<V> tap(final Pool pool, final DataflowWriteChannel<V> target) {
        return tap(new DefaultPGroup(pool), target);
    }

    @Override
    public <V> DataflowReadChannel<V> tap(final PGroup group, final DataflowWriteChannel<V> target) {
        final DataflowQueue<V> result = new DataflowQueue<V>();
        group.operator(asList(this), asList(result, target), new ChainWithClosure(new CopyChannelsClosure()));
        return result;
    }

    @Override
    public <V> DataflowReadChannel<V> merge(final DataflowReadChannel<Object> other, final Closure closure) {
        return merge(asList(other), closure);
    }

    @Override
    public <V> DataflowReadChannel<V> merge(final Pool pool, final DataflowReadChannel<Object> other, final Closure closure) {
        return merge(pool, asList(other), closure);
    }

    @Override
    public <V> DataflowReadChannel<V> merge(final PGroup group, final DataflowReadChannel<Object> other, final Closure closure) {
        return merge(group, asList(other), closure);
    }

    @Override
    public <V> DataflowReadChannel<V> merge(final List<DataflowReadChannel<Object>> others, final Closure closure) {
        return merge(Dataflow.DATA_FLOW_GROUP, others, closure);
    }

    @Override
    public <V> DataflowReadChannel<V> merge(final Pool pool, final List<DataflowReadChannel<Object>> others, final Closure closure) {
        return merge(new DefaultPGroup(pool), others, closure);
    }

    @Override
    public <V> DataflowReadChannel<V> merge(final PGroup group, final List<DataflowReadChannel<Object>> others, final Closure closure) {
        final DataflowQueue<V> result = new DataflowQueue<V>();
        final List<DataflowReadChannel> inputs = new ArrayList<DataflowReadChannel>();
        inputs.add(this);
        inputs.addAll(others);
        group.operator(inputs, asList(result), new ChainWithClosure(closure));
        return result;
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

