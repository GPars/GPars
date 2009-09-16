package org.gparallelizer.remote.serial;

import org.codehaus.groovy.util.ManagedReference;
import org.codehaus.groovy.util.ReferenceManager;
import org.gparallelizer.remote.RemoteHost;
import org.gparallelizer.remote.RemoteTransportProvider;

import java.util.*;

/**
* @author Alex Tkachman
*/
public class SerialHandle extends ManagedReference<WithSerialId> {
    protected final UUID serialId;

    protected final UUID hostId;

    private final RemoteTransportProvider transportProvider;
    private volatile Object subscribers;

    public SerialHandle(WithSerialId value) {
        this(value, null);
    }

    public SerialHandle(WithSerialId value, UUID id) {
        super(ReferenceManager.getDefaultWeakBundle(), value);

        final RemoteHost host = RemoteHost.getThreadContext();
        this.transportProvider = host.getProvider();
        if (id == null) {
            serialId = UUID.randomUUID();
            hostId = host.getProvider().getId();
        }
        else {
            serialId = id;
            hostId = host.getId();
        }
        host.getProvider().localHandles.put(serialId, this);
    }

    public UUID getSerialId() {
        return serialId;
    }

    @Override
    public void finalizeReference() {
        transportProvider.finalizeHandle(this);
        super.finalizeReference();
    }

    public UUID getHostId() {
        return hostId;
    }

    public Object getSubscribers() {
        return subscribers;
    }

    public void subscribe (RemoteHost host) {
        synchronized (this) {
            if (subscribers == null) {
                subscribers = host;
            }
            else {
                if (subscribers instanceof RemoteHost) {
                    if (subscribers != host) {
                        final ArrayList<RemoteHost> list = new ArrayList<RemoteHost>(2);
                        list.add((RemoteHost) subscribers);
                        list.add(host);
                        subscribers = list;
                    }
                }
                else {
                    @SuppressWarnings({"unchecked"})
                    final List<RemoteHost> list = (List<RemoteHost>) subscribers;
                    for (RemoteHost remoteHost : list) {
                        if (remoteHost == host)
                            return;
                    }
                    list.add(host);
                }
            }
        }
    }
}
