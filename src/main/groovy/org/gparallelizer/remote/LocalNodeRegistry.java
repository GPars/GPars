package org.gparallelizer.remote;

import org.gparallelizer.remote.memory.SharedMemoryTransportProvider;

import java.util.*;

public class LocalNodeRegistry {
    public static final Set<RemoteTransportProvider> transportProviders
            = Collections.synchronizedSet(new HashSet<RemoteTransportProvider>(Arrays.asList(SharedMemoryTransportProvider.getInstance())));

    public synchronized static void connect(final LocalNode node) {
        for (final RemoteTransportProvider transportProvider : transportProviders) {
            node.connect(transportProvider);
        }
    }

    public synchronized static void disconnect(final LocalNode node) {
        for (final RemoteTransportProvider transportProvider : transportProviders) {
            node.getScheduler().execute(new Runnable(){
                public void run() {
                    transportProvider.disconnect(node);
                }
            });
        }
    }

    public static synchronized void removeTransportProvider (RemoteTransportProvider provider) {
        transportProviders.remove(provider);
    }

    public static synchronized void addTransportProvider (RemoteTransportProvider provider) {
        transportProviders.add(provider);
    }
}
