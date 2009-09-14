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

import groovy.time.Duration;
import org.gparallelizer.actors.Actor;
import org.gparallelizer.actors.ReplyRegistry;
import org.gparallelizer.MessageStream;
import org.gparallelizer.remote.serial.RemoteSerialized;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Actor redirecting all calls to remote nodes
 * 
 * @author Alex Tkachman
 */
public class RemoteActor extends Actor implements RemoteSerialized {
    private RemoteHost remoteHost;

    public RemoteActor() {
    }

    public Actor start() {
        throw new UnsupportedOperationException();
    }

    public Actor stop() {
        remoteHost.stop(this);
        return this;
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

    public MessageStream send(Object message) {
        remoteHost.send(this, message);
        return this;
    }

    @Override
    public void initDeserial(UUID serialId) {
        remoteHost = RemoteHost.getThreadContext();
        this.serialId = serialId;
    }
}
