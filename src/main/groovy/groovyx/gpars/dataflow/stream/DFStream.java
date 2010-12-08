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
import groovyx.gpars.dataflow.DataFlowChannel;
import groovyx.gpars.dataflow.DataFlowExpression;
import groovyx.gpars.dataflow.DataFlowReadChannel;
import groovyx.gpars.dataflow.DataFlowVariable;
import groovyx.gpars.dataflow.DataFlowWriteChannel;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

//todo unchecked casts
//todo a thread-safe variant - multiple writers and readers
@SuppressWarnings({"rawtypes", "TailRecursion", "RawUseOfParameterizedType", "unchecked"})
public class DFStream<T> implements DataFlowChannel<T> {

    private Stream<T> head;
    private Stream<T> tail;

    public DFStream() {
        this(new Stream<T>());
    }

    public DFStream(final Stream<T> stream) {
        this.head = stream;
        this.tail = stream;
    }

    @Override
    public DataFlowWriteChannel<T> leftShift(final DataFlowReadChannel<T> ref) {
        head = head.leftShift(ref);
        return this;
    }

    @Override
    public DataFlowWriteChannel<T> leftShift(final T value) {
        head = head.leftShift(value);
        return this;
    }

    @Override
    public void bind(final T value) {
        head = head.leftShift(value);
    }

    //todo consider adding to the interface
    public Iterator<T> iterator() {
        return new FListIterator<T>(tail);
    }

    @Override
    public String toString() {
        return tail.toString();
    }

    @Override
    public T getVal() throws InterruptedException {
        final T first = tail.getFirst();
        tail = (Stream<T>) tail.getRest();
        return first;
    }

    @Override
    public T getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        tail.getFirstDFV().getVal(timeout, units);
        if (tail.getFirstDFV().isBound()) {
            final T result = tail.getFirst();
            tail = (Stream<T>) tail.getRest();
            return result;
        } else {
            return null;
        }
    }

    @Override
    public void getValAsync(final MessageStream callback) {
        tail.getFirstDFV().getValAsync(callback);
    }

    @Override
    public void getValAsync(final Object attachment, final MessageStream callback) {
        tail.getFirstDFV().getValAsync(attachment, callback);
    }

    @Override
    public void rightShift(final Closure closure) {
        tail.getFirstDFV().rightShift(closure);
    }

    @Override
    public void whenBound(final Closure closure) {
        tail.getFirstDFV().whenBound(closure);
    }

    @Override
    public void whenBound(final MessageStream stream) {
        this.tail.getFirstDFV().whenBound(stream);
    }

    //todo provide implementation
    @Override
    public void wheneverBound(final Closure closure) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void wheneverBound(final MessageStream stream) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isBound() {
        return tail.getFirstDFV().isBound();
    }

    @Override
    public DataFlowExpression<T> poll() throws InterruptedException {
        final DataFlowVariable<T> firstDFV = tail.getFirstDFV();
        if (firstDFV.isBound()) {
            tail = (Stream<T>) tail.getRest();
            return firstDFV;
        } else return null;
    }
}

