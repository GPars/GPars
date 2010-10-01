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
import groovyx.gpars.dataflow.DataFlowVariable;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"TailRecursion"})
public class Stream<T> implements FList<T>, DataFlowChannel {

    private final DataFlowVariable<T> first = new DataFlowVariable<T>();
    private final AtomicReference<Stream<T>> rest = new AtomicReference<Stream<T>>();

    public static <T> T eos() {
        return null;
    }

    private static <T> T eval(final Object valueOrDataFlowVariable) {
        if (valueOrDataFlowVariable instanceof DataFlowVariable)
            try {
                return ((DataFlowVariable<T>) valueOrDataFlowVariable).getVal();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        return (T) valueOrDataFlowVariable;
    }

    public Stream() {
    }

    public Stream(final Closure toBeApplied) {
        apply(toBeApplied);
    }

    public Stream<T> generate(final T seed, final Closure generator, final Closure condition) {
        generateNext(seed, this, generator, condition);
        return this;
    }

    private void generateNext(final T value, final Stream<T> stream, final Closure generator, final Closure condition) {
        final boolean addValue = (Boolean) condition.call(new Object[]{value});
        if (!addValue) {
            stream.leftShift(Stream.<T>eos());
            return;
        }
        final Stream<T> next = stream.leftShift(value);
        final T nextValue = (T) eval(generator.call(new Object[]{value}));
        generateNext(nextValue, next, generator, condition);
    }

    public final Stream<T> apply(final Closure closure) {
        closure.call(new Object[]{this});
        return this;
    }

    public Stream<T> leftShift(final DataFlowVariable<T> df) throws InterruptedException {
        return leftShift(df.getVal());
    }

    public Stream<T> leftShift(final T value) {
        first.leftShift(value);
        return (Stream<T>) getRest();
    }

    @Override
    public T getFirst() {
        try {
            return getVal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FList<T> getRest() {
        if (rest.get() == null)
            rest.compareAndSet(null, new Stream());
        return rest.get();
    }

    @Override
    public boolean isEmpty() {
        return getFirst() == eos();
    }

    @Override
    public FList<T> filter(final Closure filterClosure) {
        final Stream<T> newStream = new Stream<T>();
        filter(this, filterClosure, newStream);
        return newStream;
    }

    //todo shouldn't we be lazy here?
    private void filter(final Stream<T> rest, final Closure filterClosure, final Stream<T> result) {
        if (rest.isEmpty()) {
            result.leftShift(Stream.<T>eos());
            return;
        }
        final boolean include = (Boolean) eval(filterClosure.call(new Object[]{rest.getFirst()}));
        if (include)
            filter((Stream<T>) rest.getRest(), filterClosure, result.leftShift(rest.getFirst()));
        else
            filter((Stream<T>) rest.getRest(), filterClosure, result);
    }

    @Override
    public FList<?> map(final Closure mapClosure) {
        final Stream<?> newStream = new Stream();
        map(this, mapClosure, newStream);
        return newStream;
    }

    private void map(final Stream rest, final Closure mapClosure, final Stream result) {
        if (rest.isEmpty()) {
            result.leftShift(Stream.eos());
            return;
        }
        final Object mapped = mapClosure.call(new Object[]{rest.getFirst()});
        final Stream newResult = result.leftShift(eval(mapped));
        map((Stream) rest.getRest(), mapClosure, newResult);
    }

    @Override
    public T reduce(final Closure reduceClosure) {
        if (isEmpty())
            return null;
        return reduce(getFirst(), getRest(), reduceClosure);
    }

    @Override
    public T reduce(final T seed, final Closure reduceClosure) {
        return reduce(seed, this, reduceClosure);
    }

    private T reduce(final T current, final FList<T> rest, final Closure reduceClosure) {
        if (rest.isEmpty())
            return current;
        final Object aggregate = reduceClosure.call(new Object[]{current, rest.getFirst()});
        return reduce((T) eval(aggregate), rest.getRest(), reduceClosure);
    }

    @Override
    public Iterator<T> iterator() {
        return new FListIterator<T>(this);
    }

    @Override
    public String toString() {
        if (!first.isBound())
            return "Stream[?]";
        if (isEmpty())
            return "Stream[]";
        return "Stream[" + getFirst() + ((Stream) getRest()).appendingString() + ']';
    }

    private String appendingString() {
        if (!first.isBound())
            return ", ?";
        if (isEmpty())
            return "";
        return ", " + getFirst() + ((Stream) getRest()).appendingString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final Stream stream = (Stream) obj;
        if (isEmpty())
            return stream.isEmpty();
        if (!getFirst().equals(stream.getFirst()))
            return false;
        return getRest().equals(stream.getRest());
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + rest.hashCode();
        return result;
    }

    @Override
    public T getVal() throws InterruptedException {
        return first.getVal();
    }

    @Override
    public T getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        return first.getVal(timeout, units);
    }

    @Override
    public void getValAsync(final MessageStream callback) {
        first.getValAsync(callback);
    }

    @Override
    public void getValAsync(final Object attachment, final MessageStream callback) {
        first.getValAsync(attachment, callback);
    }

    @Override
    public void rightShift(final Closure closure) {
        first.rightShift(closure);
    }

    @Override
    public void whenBound(final Closure closure) {
        first.whenBound(closure);
    }

    @Override
    public void whenBound(final MessageStream stream) {
        first.whenBound(stream);
    }
}

