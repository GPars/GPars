package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteActorFuture implements Future<Actor> {
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
        return false;
    }

    @Override
    public Actor get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public Actor get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
