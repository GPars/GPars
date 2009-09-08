package org.gparallelizer.remote;

import org.gparallelizer.remote.sharedmemory.SharedMemoryTransportProvider;
import org.gparallelizer.remote.nonsharedmemory.NonSharedMemoryTransportProvider;

import java.util.*;

public class LocalNodeRegistry {
    public static final Set<RemoteTransportProvider> transportProviders
            = Collections.synchronizedSet(new HashSet<RemoteTransportProvider>());

    public synchronized static void connect(final LocalNode node) {
        if (transportProviders.isEmpty())
            transportProviders.add(SharedMemoryTransportProvider.getInstance());

        for (final RemoteTransportProvider transportProvider : transportProviders) {
            node.getScheduler().execute(new Runnable(){
                public void run() {
                    transportProvider.connect(node);
                }
            });
        }
    }

    public synchronized static void disconnect(final LocalNode node) {
        if (transportProviders.isEmpty())
            transportProviders.add(SharedMemoryTransportProvider.getInstance());

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
