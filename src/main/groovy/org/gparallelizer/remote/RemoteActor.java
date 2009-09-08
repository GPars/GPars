//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.remote;

import org.gparallelizer.actors.Actor;
import org.gparallelizer.actors.ActorMessage;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

import groovy.time.Duration;

public class RemoteActor implements Actor {
    private final RemoteNode remoteNode;

    private       UUID       id;

    public RemoteActor(RemoteNode remoteNode, UUID id) {
        this.remoteNode = remoteNode;
        this.id = id;
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
        remoteNode.send(this, ActorMessage.build(message));
        return this;
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

    public void setId(UUID id) {
        this.id = id;
    }
}
