package org.gparallelizer.remote.memory;

import org.gparallelizer.remote.RemoteActor;
import org.gparallelizer.remote.RemoteNode;
import org.gparallelizer.actors.Actor;

import java.util.concurrent.TimeUnit;

import groovy.time.Duration;

public class SharedMemoryActor extends RemoteActor {
    private final Actor delegate;

    public SharedMemoryActor(RemoteNode remoteNode, Actor delegate) {
        super(remoteNode, null);
        this.delegate = delegate;
    }

    @Override
    public Actor start() {
        return delegate.start();
    }

    @Override
    public Actor stop() {
        return delegate.stop();
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
    }

    @Override
    public boolean isActorThread() {
        return delegate.isActorThread();
    }

    @Override
    public void join() {
        delegate.join();
    }

    @Override
    public void join(long milis) {
        delegate.join(milis);
    }

    @Override
    public Actor send(Object message) {
        return delegate.send(message);
    }

    @Override
    public Object sendAndWait(Object message) {
        return delegate.sendAndWait(message);
    }

    @Override
    public Object sendAndWait(long timeout, TimeUnit timeUnit, Object message) {
        return delegate.sendAndWait(timeout, timeUnit, message);
    }

    @Override
    public Object sendAndWait(Duration duration, Object message) {
        return super.sendAndWait(duration, message);
    }

    @Override
    public Actor leftShift(Object message) {
        return super.leftShift(message);
    }

    @Override
    public RemoteNode getRemoteNode() {
        return super.getRemoteNode();
    }

    public Actor getDelegate() {
        return delegate;
    }
}
