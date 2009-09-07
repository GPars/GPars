package org.gparallelizer.remote;

import org.gparallelizer.actors.ActorMessage;
import org.gparallelizer.actors.Actor;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public abstract void onDisconnect(RemoteNode smn);

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

    protected abstract RemoteActor createRemoteActor(UUID uid);

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
        try {
           byte [] data = encodeMessage(receiver, message);
           deliver(data);
        }
        catch (IOException e) { //
        }
    }

    protected byte[] encodeMessage(RemoteActor receiver, ActorMessage<Serializable> message) throws IOException {
        final RemoteMessage toSend = new RemoteMessage(receiver.getId(), getLocalActorId(message.getSender()), message.getPayLoad());
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(toSend);
            oout.close();
            return bout.toByteArray();
        } catch (IOException e) {
            throw e;
        }
    }

    protected RemoteMessage decodeMessage (byte [] data) {
        try {
            return (RemoteMessage) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
        } catch (Throwable e) {
            return null;
        }
    }

    public Actor getLocalActor(UUID uuid) {
        return localActorsId.get(uuid);
    }
}
