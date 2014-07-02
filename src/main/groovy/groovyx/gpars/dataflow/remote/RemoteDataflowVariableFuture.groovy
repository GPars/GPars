package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowVariable

import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class RemoteDataflowVariableFuture implements Future<DataflowVariable> {
    @Override
    boolean cancel(boolean mayInterruptIfRunning) {
        return false
    }

    @Override
    boolean isCancelled() {
        return false
    }

    @Override
    boolean isDone() {
        return false
    }

    @Override
    DataflowVariable get() throws InterruptedException, ExecutionException {
        return null
    }

    @Override
    DataflowVariable get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null
    }
}
