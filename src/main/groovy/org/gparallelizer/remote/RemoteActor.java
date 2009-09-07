package org.gparallelizer.remote;

import org.gparallelizer.actors.Actor;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

import groovy.time.Duration;

public class RemoteActor implements Actor {
    private final RemoteNode remoteNode;
    private       UUID       id;

    public RemoteActor(RemoteNode remoteNode) {
        this.remoteNode = remoteNode;
    }

    public Actor start() {
        throw new UnsupportedOperationException();
    }

    public Actor stop() {
        throw new UnsupportedOperationException();
    }

    public boolean isActive() {
        throw new UnsupportedOperationException();
    }

    public boolean isActorThread() {
        throw new UnsupportedOperationException();
    }

    public void join() {
        throw new UnsupportedOperationException();
    }

    public void join(long milis) {
        throw new UnsupportedOperationException();
    }

    public Actor send(Object message) {
        throw new UnsupportedOperationException();
    }

    public Object sendAndWait(Object message) {
        throw new UnsupportedOperationException();
    }

    public Object sendAndWait(long timeout, TimeUnit timeUnit, Object message) {
        throw new UnsupportedOperationException();
    }

    public Object sendAndWait(Duration duration, Object message) {
        throw new UnsupportedOperationException();
    }

    public Actor leftShift(Object message) {
        return send (message);
    }

    public RemoteNode getRemoteNode() {
        return remoteNode;
    }

    public UUID getId() {
        if (id == null)
          id = UUID.randomUUID();
        return id;
    }
}
