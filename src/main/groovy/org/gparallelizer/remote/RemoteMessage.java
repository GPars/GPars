package org.gparallelizer.remote;

import java.io.Serializable;
import java.util.UUID;

public class RemoteMessage implements Serializable {
    final UUID         to;
    final UUID         from;
    final Serializable payload;

    public RemoteMessage(UUID to, UUID from, Serializable payload) {
        this.to = to;
        this.from = from;
        this.payload = payload;
    }
}
