package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable

import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class RemoteDataflowQueueFuture implements Future<DataflowQueue<?>> {
    final DataflowVariable<DataflowQueue<?>> remoteQueue;

    RemoteDataflowQueueFuture(DataflowVariable<DataflowQueue<?>> remoteQueue) {
        this.remoteQueue = remoteQueue
    }

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
    DataflowQueue<?> get() throws InterruptedException, ExecutionException {
        return null
    }

    @Override
    DataflowQueue<?> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null
    }
}
