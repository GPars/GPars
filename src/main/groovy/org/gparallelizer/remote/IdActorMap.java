package org.gparallelizer.remote;

import org.gparallelizer.actors.Actor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class IdActorMap extends ConcurrentHashMap<Actor, UUID> {
    private final UUID dummy = UUID.randomUUID();

    public UUID getId(Actor id) {
        if (id == null) {
            return null;
        }

        UUID uuid = putIfAbsent(id, dummy);
        if (uuid == null || uuid == dummy) {
            synchronized (this) {
                uuid = get(id);
                if (uuid == null || uuid == dummy) {
                    uuid = UUID.randomUUID();
                    put(id, uuid);
                }
            }
        }
        return uuid;
    }
}
