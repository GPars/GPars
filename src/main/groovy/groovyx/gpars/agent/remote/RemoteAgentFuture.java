package groovyx.gpars.agent.remote;

import groovyx.gpars.agent.Agent;
import groovyx.gpars.dataflow.DataflowVariable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteAgentFuture implements Future<RemoteAgent<?>> {
    private final DataflowVariable<RemoteAgent<?>> agentVariable;

    public RemoteAgentFuture(DataflowVariable<RemoteAgent<?>> agentVariable) {
        this.agentVariable = agentVariable;
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
        return agentVariable.isBound();
    }

    @Override
    public RemoteAgent<?> get() throws InterruptedException, ExecutionException {
        try {
            return agentVariable.get();
        } catch (Throwable throwable) {
            throw new ExecutionException(throwable);
        }
    }

    @Override
    public RemoteAgent<?> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return agentVariable.get(timeout, unit);
        } catch (Throwable throwable) {
            throw new ExecutionException(throwable);
        }
    }
}
