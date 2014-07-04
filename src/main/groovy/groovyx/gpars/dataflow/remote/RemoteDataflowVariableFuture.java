package groovyx.gpars.dataflow.remote;

import groovyx.gpars.dataflow.DataflowVariable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteDataflowVariableFuture implements Future<DataflowVariable> {
    final DataflowVariable<DataflowVariable> remoteVariable;

    public RemoteDataflowVariableFuture(DataflowVariable<DataflowVariable> remoteVariable) {
        this.remoteVariable = remoteVariable;
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
        return remoteVariable.isBound();
    }

    @Override
    public DataflowVariable get() throws InterruptedException, ExecutionException {
        try {
            return remoteVariable.get();
        } catch (Throwable throwable) {
            throw new ExecutionException(throwable);
        }
    }

    @Override
    public DataflowVariable get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return remoteVariable.get(timeout, unit);
        } catch (Throwable throwable) {
            throw new ExecutionException(throwable);
        }
    }
}
