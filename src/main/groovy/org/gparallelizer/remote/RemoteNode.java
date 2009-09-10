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
import org.gparallelizer.remote.messages.MessageToActor;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representation of remote node
 *
 * @author Alex Tkachman
 */
 public abstract class RemoteNode<T extends RemoteTransportProvider> {
    protected final static RemoteActor DUMMY = new RemoteActor(null,null);

    // TODO: DANGER of memory leak
    private final ConcurrentHashMap<UUID,RemoteActor> remoteActors = new ConcurrentHashMap<UUID,RemoteActor> ();

    private final UUID id;

    private final T provider;

    public RemoteNode(UUID id, T provider) {
        this.id = id;
        this.provider = provider;
    }

    public final UUID getId () {
        return id;
    }

    public abstract void onConnect(LocalNode node);

    public abstract void onDisconnect(LocalNode node);

    @Override
    public String toString() {
        return getId().toString();
    }

    public final RemoteActor getMainActor () {
        return getRemoteActor (getId());
    }

    protected abstract void deliver(byte[] bytes) throws IOException;

    protected void send(RemoteActor receiver, Serializable message, Actor sender) {
        byte [] data = new byte[0];
        try {
            data = encodeMessage(receiver, message, sender);
            deliver(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected byte[] encodeMessage(RemoteActor receiver, Serializable message, Actor sender) throws IOException {
        final MessageToActor toSend = createMessage(receiver, message, sender);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(toSend);
        oout.close();
        return bout.toByteArray();
    }

    protected MessageToActor createMessage(RemoteActor receiver, Serializable message, Actor sender) {
        return new MessageToActor(getId(), receiver.getId(), provider.getLocalActorId(sender), message);
    }

    protected MessageToActor decodeMessage (byte [] data) throws IOException {
        try {
            return (MessageToActor) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    protected RemoteActor getRemoteActor(UUID uid) {
        if (uid == null) {
            return null;
        }

        RemoteActor actor = remoteActors.putIfAbsent(uid, DUMMY);
        if (actor == null || actor == DUMMY) {
            synchronized (remoteActors) {
                actor = remoteActors.get(uid);
                if (actor == null || actor == DUMMY) {
                    actor = createRemoteActor(uid);
                    remoteActors.put(uid, actor);
                }
            }
        }
        return actor;
    }

    protected abstract RemoteActor createRemoteActor(UUID uid);

    public T getProvider() {
        return provider;
    }
}
