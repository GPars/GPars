package groovyx.gpars.dataflow.remote;

import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.DataflowVariable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteDataflowQueueFuture implements Future<RemoteDataflowQueue<?>> {
    private final DataflowVariable<RemoteDataflowQueue<?>> remoteQueue;

    public RemoteDataflowQueueFuture(DataflowVariable<RemoteDataflowQueue<?>> remoteQueue) {
        this.remoteQueue = remoteQueue;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return remoteQueue.isBound();
    }

    @Override
    public RemoteDataflowQueue<?> get() throws InterruptedException, ExecutionException {
        try {
            return remoteQueue.get();
        } catch (Throwable throwable) {
            throw new ExecutionException(throwable);
        }
    }

    @Override
    public RemoteDataflowQueue<?> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return remoteQueue.get(timeout, unit);
        } catch (Throwable throwable) {
            throw new ExecutionException(throwable);
        }
    }

}
