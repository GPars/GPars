package org.gparallelizer.remote.serial;

import org.gparallelizer.remote.RemoteHost;

import java.util.UUID;
import java.io.Serializable;
import java.io.ObjectStreamException;

/**
 * @author Alex Tkachman
 */
public final class LocalHandle implements Serializable {
    private UUID id;

    public LocalHandle(UUID id) {
        this.id = id;
    }

    protected Object readResolve () throws ObjectStreamException {
        return RemoteHost.getThreadContext().readResolve(this);
    }

    public UUID getId() {
        return id;
    }
}
