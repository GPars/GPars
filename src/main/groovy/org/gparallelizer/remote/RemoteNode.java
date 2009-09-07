package org.gparallelizer.remote;

import org.gparallelizer.actors.ActorMessage;
import org.gparallelizer.remote.sharedmemory.SharedMemoryNode;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representation of remote node
 */
public abstract class RemoteNode {
    public final static UUID MAIN_ACTOR_ID = new UUID(0L,0L);

    private final static RemoteActor DUMMY = new RemoteActor(null);

    private final ConcurrentHashMap<UUID,RemoteActor> remoteActors = new ConcurrentHashMap<UUID,RemoteActor> ();

    public RemoteNode() {
    }

    public abstract UUID getId ();

    public abstract void send(RemoteActor receiver, ActorMessage<Serializable> message);

    public abstract void onDisconnect(SharedMemoryNode smn);

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
}
