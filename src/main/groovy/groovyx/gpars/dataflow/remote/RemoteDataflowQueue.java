package groovyx.gpars.dataflow.remote;


import groovy.lang.Closure;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.dataflow.DataflowChannel;
import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.DataflowWriteChannel;
import groovyx.gpars.dataflow.Promise;
import groovyx.gpars.dataflow.expression.DataflowExpression;
import groovyx.gpars.dataflow.impl.DataflowChannelEventListenerManager;
import groovyx.gpars.group.PGroup;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.scheduler.Pool;
import groovyx.gpars.serial.RemoteSerialized;
import groovyx.gpars.serial.WithSerialId;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RemoteDataflowQueue<T> extends WithSerialId implements DataflowChannel<T>, RemoteSerialized {
    public RemoteDataflowQueue(RemoteHost host) {
        System.err.println("RemoteDataflowQueue");
    }

    @Override
    public void getValAsync(MessageStream callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getValAsync(Object attachment, MessageStream callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T getVal() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public T getVal(long timeout, TimeUnit units) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> Promise<V> rightShift(Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> void whenBound(Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> void whenBound(Pool pool, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> void whenBound(PGroup group, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void whenBound(MessageStream stream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> Promise<V> then(Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> Promise<V> then(Pool pool, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> Promise<V> then(PGroup group, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> chainWith(Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> chainWith(Pool pool, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> chainWith(PGroup group, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> chainWith(Map<String, Object> params, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> chainWith(Pool pool, Map<String, Object> params, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> chainWith(PGroup group, Map<String, Object> params, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> or(Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> filter(Closure<Boolean> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> filter(Pool pool, Closure<Boolean> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> filter(PGroup group, Closure<Boolean> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> filter(Map<String, Object> params, Closure<Boolean> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> filter(Pool pool, Map<String, Object> params, Closure<Boolean> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> filter(PGroup group, Map<String, Object> params, Closure<Boolean> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void into(DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void into(Pool pool, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void into(PGroup group, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void into(Map<String, Object> params, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void into(Pool pool, Map<String, Object> params, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void into(PGroup group, Map<String, Object> params, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void or(DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(DataflowWriteChannel<T> target1, DataflowWriteChannel<T> target2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(Pool pool, DataflowWriteChannel<T> target1, DataflowWriteChannel<T> target2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(PGroup group, DataflowWriteChannel<T> target1, DataflowWriteChannel<T> target2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(List<DataflowWriteChannel<T>> targets) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(Pool pool, List<DataflowWriteChannel<T>> targets) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(PGroup group, List<DataflowWriteChannel<T>> targets) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(Map<String, Object> params, DataflowWriteChannel<T> target1, DataflowWriteChannel<T> target2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(Pool pool, Map<String, Object> params, DataflowWriteChannel<T> target1, DataflowWriteChannel<T> target2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(PGroup group, Map<String, Object> params, DataflowWriteChannel<T> target1, DataflowWriteChannel<T> target2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(Map<String, Object> params, List<DataflowWriteChannel<T>> targets) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(Pool pool, Map<String, Object> params, List<DataflowWriteChannel<T>> targets) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void split(PGroup group, Map<String, Object> params, List<DataflowWriteChannel<T>> targets) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> tap(DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> tap(Pool pool, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> tap(PGroup group, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> tap(Map<String, Object> params, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> tap(Pool pool, Map<String, Object> params, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowReadChannel<T> tap(PGroup group, Map<String, Object> params, DataflowWriteChannel<T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(DataflowReadChannel<Object> other, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(Pool pool, DataflowReadChannel<Object> other, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(PGroup group, DataflowReadChannel<Object> other, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(List<DataflowReadChannel<Object>> others, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(Pool pool, List<DataflowReadChannel<Object>> others, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(PGroup group, List<DataflowReadChannel<Object>> others, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(Map<String, Object> params, DataflowReadChannel<Object> other, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(Pool pool, Map<String, Object> params, DataflowReadChannel<Object> other, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(PGroup group, Map<String, Object> params, DataflowReadChannel<Object> other, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(Map<String, Object> params, List<DataflowReadChannel<Object>> others, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(Pool pool, Map<String, Object> params, List<DataflowReadChannel<Object>> others, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> DataflowReadChannel<V> merge(PGroup group, Map<String, Object> params, List<DataflowReadChannel<Object>> others, Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void binaryChoice(DataflowWriteChannel<T> trueBranch, DataflowWriteChannel<T> falseBranch, Closure<Boolean> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void binaryChoice(Pool pool, DataflowWriteChannel<T> trueBranch, DataflowWriteChannel<T> falseBranch, Closure<Boolean> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void binaryChoice(PGroup group, DataflowWriteChannel<T> trueBranch, DataflowWriteChannel<T> falseBranch, Closure<Boolean> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void binaryChoice(Map<String, Object> params, DataflowWriteChannel<T> trueBranch, DataflowWriteChannel<T> falseBranch, Closure<Boolean> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void binaryChoice(Pool pool, Map<String, Object> params, DataflowWriteChannel<T> trueBranch, DataflowWriteChannel<T> falseBranch, Closure<Boolean> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void binaryChoice(PGroup group, Map<String, Object> params, DataflowWriteChannel<T> trueBranch, DataflowWriteChannel<T> falseBranch, Closure<Boolean> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void choice(List<DataflowWriteChannel<T>> outputs, Closure<Integer> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void choice(Pool pool, List<DataflowWriteChannel<T>> outputs, Closure<Integer> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void choice(PGroup group, List<DataflowWriteChannel<T>> outputs, Closure<Integer> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void choice(Map<String, Object> params, List<DataflowWriteChannel<T>> outputs, Closure<Integer> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void choice(Pool pool, Map<String, Object> params, List<DataflowWriteChannel<T>> outputs, Closure<Integer> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void choice(PGroup group, Map<String, Object> params, List<DataflowWriteChannel<T>> outputs, Closure<Integer> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void separate(List<DataflowWriteChannel<?>> outputs, Closure<List<Object>> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void separate(Pool pool, List<DataflowWriteChannel<?>> outputs, Closure<List<Object>> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void separate(PGroup group, List<DataflowWriteChannel<?>> outputs, Closure<List<Object>> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void separate(Map<String, Object> params, List<DataflowWriteChannel<?>> outputs, Closure<List<Object>> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void separate(Pool pool, Map<String, Object> params, List<DataflowWriteChannel<?>> outputs, Closure<List<Object>> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void separate(PGroup group, Map<String, Object> params, List<DataflowWriteChannel<?>> outputs, Closure<List<Object>> code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowChannelEventListenerManager<T> getEventManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBound() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int length() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowWriteChannel<T> leftShift(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowWriteChannel<T> leftShift(DataflowReadChannel<T> ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V> void wheneverBound(Closure<V> closure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void wheneverBound(MessageStream stream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataflowExpression<T> poll() throws InterruptedException {
        throw new UnsupportedOperationException();
    }
}
