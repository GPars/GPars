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

import org.gparallelizer.actors.ActorMessage;
import org.gparallelizer.actors.Actor;
import org.gparallelizer.actors.pooledActors.Pool;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Representation of remote node
 */
public abstract class RemoteNode {
    public final static UUID MAIN_ACTOR_ID = new UUID(0L,0L);

    protected final static RemoteActor DUMMY = new RemoteActor(null,null);

    // TODO: DANGER of memory leak
    private final ConcurrentHashMap<UUID,RemoteActor> remoteActors = new ConcurrentHashMap<UUID,RemoteActor> ();

    // TODO: DANGER of memory leak
    private final ConcurrentHashMap<Actor,UUID> localActors = new ConcurrentHashMap<Actor, UUID>();

    protected final ConcurrentHashMap<UUID,Actor> localActorsId = new ConcurrentHashMap<UUID, Actor>();

    public RemoteNode() {
    }

    public abstract UUID getId ();

    public abstract void onConnect(RemoteNode node);

    public abstract void onDisconnect(RemoteNode node);

    @Override
    public String toString() {
        return getId().toString();
    }

    public final RemoteActor getMainActor () {
        return getRemoteActor (MAIN_ACTOR_ID);
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

    protected RemoteActor createRemoteActor(UUID uid) {
        if (uid == RemoteNode.MAIN_ACTOR_ID)
            return new RemoteActor(this, uid);

        throw new UnsupportedOperationException();
    }

    protected abstract void deliver(byte[] bytes) throws IOException;

    protected UUID getLocalActorId (Actor actor) {
        if (actor == null) {
            return null;
        }

        UUID uid = localActors.putIfAbsent(actor, MAIN_ACTOR_ID);
        if (uid == null || uid == MAIN_ACTOR_ID) {
            synchronized (localActors) {
                uid = localActors.get(actor);
                if (uid == null || uid == MAIN_ACTOR_ID) {
                    uid = UUID.randomUUID();
                    localActors.put(actor, uid);
                    localActorsId.put(uid, actor);
                }
            }
        }
        return uid;
    }

    protected void send(RemoteActor receiver, ActorMessage<Serializable> message) {
        byte [] data = new byte[0];
        try {
            data = encodeMessage(receiver, message);
            deliver(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected byte[] encodeMessage(RemoteActor receiver, ActorMessage<Serializable> message) throws IOException {
        final RemoteMessage toSend = new RemoteMessage(receiver.getId(), getLocalActorId(message.getSender()), message.getPayLoad());
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(toSend);
        oout.close();
        return bout.toByteArray();
    }

    protected RemoteMessage decodeMessage (byte [] data) throws IOException {
        try {
            return (RemoteMessage) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    public Actor getLocalActor(UUID uuid) {
        return localActorsId.get(uuid);
    }

    protected final void onMessageReceived(byte [] bytes, final Executor scheduler) throws IOException {
        final RemoteMessage remoteMessage = decodeMessage(bytes);
        scheduler.execute(new Runnable(){
            public void run() {
                getLocalActor(remoteMessage.getTo()).send(remoteMessage.getPayload());
            }
        });
    }
}
