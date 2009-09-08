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
