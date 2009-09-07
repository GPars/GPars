package org.gparallelizer.remote;

import java.io.Serializable;
import java.util.UUID;

public class RemoteMessage implements Serializable {
    private final UUID         to;
    private final UUID         from;
    private final Serializable payload;

    public RemoteMessage(UUID to, UUID from, Serializable payload) {
        this.to = to;
        this.from = from;
        this.payload = payload;
    }

    public UUID getTo() {
        return to;
    }

    public UUID getFrom() {
        return from;
    }

    public Serializable getPayload() {
        return payload;
    }
}
