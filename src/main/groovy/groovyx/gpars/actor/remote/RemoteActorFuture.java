package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.remote.LocalHost;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemoteActorFuture implements Future<Actor> {
    private final LocalHost localHost;
    private final String name;

    public RemoteActorFuture(LocalHost localHost, String name) {
        this.localHost = localHost;
        this.name = name;
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
        return localHost.getRemoteActor(name) != null;
    }

    @Override
    public Actor get() throws InterruptedException, ExecutionException {
        synchronized (this) {
            while (!isDone()) {
                wait();
            }
        }
        return localHost.getRemoteActor(name);
    }

    @Override
    public Actor get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("not yet implemented");
    }


}
