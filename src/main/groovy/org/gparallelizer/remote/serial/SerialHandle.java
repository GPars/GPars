package org.gparallelizer.remote.serial;

import org.codehaus.groovy.util.ManagedReference;
import org.codehaus.groovy.util.ReferenceManager;
import org.gparallelizer.remote.RemoteHost;
import org.gparallelizer.remote.RemoteTransportProvider;

import java.util.UUID;

/**
* @author Alex Tkachman
*/
public class SerialHandle extends ManagedReference<WithSerialId> {

    protected final UUID serialId;

    private RemoteTransportProvider transportProvider;

    public SerialHandle(WithSerialId value, RemoteTransportProvider transportProvider) {
        super(ReferenceManager.getDefaultWeakBundle(), value);
        this.transportProvider = transportProvider;
        serialId = value.serialId;
    }

    public UUID getSerialId() {
        return serialId;
    }

    @Override
    public void finalizeReference() {
        transportProvider.finalizeHandle(this);
        super.finalizeReference();
    }
}
