package groovyx.gpars.dataflow.remote;

import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.DataflowVariable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteDataflowReadChannelFuture implements Future<DataflowReadChannel> {
    private DataflowVariable<RemoteDataflowBroadcast> remoteChannel;

    public RemoteDataflowReadChannelFuture(DataflowVariable<RemoteDataflowBroadcast> remoteChannel) {
        this.remoteChannel = remoteChannel;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return remoteChannel.isBound();
    }

    @Override
    public DataflowReadChannel get() throws InterruptedException, ExecutionException {
        try {
            return remoteChannel.get().createReadChannel();
        } catch (Throwable throwable) {
            throw new ExecutionException(throwable);
        }
    }

    @Override
    public DataflowReadChannel get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return remoteChannel.get(timeout, unit).createReadChannel();
        } catch (Throwable throwable) {
            throw new ExecutionException(throwable);
        }
    }
}
