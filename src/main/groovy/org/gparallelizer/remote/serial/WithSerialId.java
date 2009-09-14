package org.gparallelizer.remote.serial;

import org.gparallelizer.remote.RemoteHost;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.UUID;

/**
 * @author Alex Tkachman
 */
public abstract class WithSerialId implements Serializable {
    public volatile UUID hostId;
    public volatile UUID serialId;

    protected final Object writeReplace () throws ObjectStreamException {
        return RemoteHost.getThreadContext().writeReplace(this);
    }

    protected final Object readResolve () throws ObjectStreamException {
        return RemoteHost.getThreadContext().readResolve(this);
    }

    public final UUID getSerialId () {
        return RemoteHost.getThreadContext().getProvider().getSerialId(this);
    }

    public void initDeserial(UUID serialId) {
        throw new UnsupportedOperationException();
    }

    public abstract Class getRemoteClass();
}
