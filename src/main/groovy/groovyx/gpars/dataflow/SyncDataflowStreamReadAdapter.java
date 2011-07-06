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
import groovyx.gpars.dataflow.stream.DataflowStreamReadAdapter;
import groovyx.gpars.dataflow.stream.StreamCore;
import groovyx.gpars.group.PGroup;
import groovyx.gpars.scheduler.Pool;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides a special implementation of DataflowStreamReadAdapter, which cooperates with SyncDataflowBroadcast subscription and un-subscription mechanism.
 *
 * @param <T> The type of messages to pass through the stream
 * @author Vaclav Pech
 */
final class SyncDataflowStreamReadAdapter<T> extends DataflowStreamReadAdapter<T> {

    private boolean closed = false;
    private boolean wheneverBoundSet = false;

    /**
     * Creates a new adapter
     *
     * @param stream The stream to wrap
     */
    SyncDataflowStreamReadAdapter(final StreamCore<T> stream) {
        super(stream);
    }

    @Override
    public Iterator<T> iterator() {
        checkClosed();
        return super.iterator();
    }

    @Override
    public T getVal() throws InterruptedException {
        checkClosed();
        return super.getVal();
    }

    @Override
    public T getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        checkClosed();
        return super.getVal(timeout, units);
    }

    @Override
    public void getValAsync(final MessageStream callback) {
        checkClosed();
        super.getValAsync(callback);
    }

    @Override
    public void getValAsync(final Object attachment, final MessageStream callback) {
        checkClosed();
        super.getValAsync(attachment, callback);
    }

    @Override
    public void rightShift(final Closure closure) {
        whenBound(closure);
    }

    @Override
    public void whenBound(final Closure closure) {
        checkClosed();
        super.whenBound(closure);
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
        checkClosed();
        super.whenBound(pool, closure);
    }

    @Override
    public void whenBound(final PGroup group, final Closure closure) {
        checkClosed();
        super.whenBound(group, closure);
    }

    @Override
    public void whenBound(final MessageStream stream) {
        checkClosed();
        super.whenBound(stream);
    }

    @Override
    public void wheneverBound(final Closure closure) {
        checkClosed();
        wheneverBoundSet = true;
        super.wheneverBound(closure);
    }

    @Override
    public void wheneverBound(final MessageStream stream) {
        checkClosed();
        wheneverBoundSet = true;
        super.wheneverBound(stream);
    }

    @Override
    public boolean isBound() {
        checkClosed();
        return super.isBound();
    }

    @Override
    public DataflowExpression<T> poll() throws InterruptedException {
        checkClosed();
        return super.poll();
    }

    private void checkClosed() {
        if (closed)
            throw new IllegalStateException("The subscription channel has already been unsubscribed and closed");
    }

    /**
     * Closes the channel so that it cannot be used any longer
     *
     * @throws InterruptedException When the thread gets interrupted
     */
    void close() throws InterruptedException {
        if (wheneverBoundSet)
            throw new IllegalStateException("The subscription cannot be closed since it has active wheneverBound handlers.");
        if (closed)
            throw new IllegalStateException("The subscription has already been closed before.");
        closed = true;
        final List<DataflowVariable<T>> dataflowVariables = allUnprocessedDFVs();
        for (final DataflowVariable<T> dataflowVariable : dataflowVariables) {
            ((SyncDataflowVariable<T>) dataflowVariable).decrementParties();
        }


    }
}

