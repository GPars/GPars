package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.LocalHost;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteActorFuture implements Future<Actor> {
    private final DataflowVariable<Actor> actor;

    public RemoteActorFuture(DataflowVariable<Actor> actor) {
        this.actor = actor;
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
        return actor.isBound();
    }

    @Override
    public Actor get() throws InterruptedException, ExecutionException {
        try {
            return actor.get();
        } catch (Throwable throwable) {
            throw new ExecutionException(throwable);
        }
    }

    @Override
    public Actor get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("not yet implemented");
    }


}
