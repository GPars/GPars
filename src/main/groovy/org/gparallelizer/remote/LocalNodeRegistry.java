package org.gparallelizer.remote;

import org.gparallelizer.remote.sharedmemory.SharedMemoryTransportProvider;
import org.gparallelizer.remote.nonsharedmemory.NonSharedMemoryTransportProvider;

import java.util.*;

public class LocalNodeRegistry {
    public static final Set<RemoteTransportProvider> transportProviders
            = Collections.synchronizedSet(new HashSet<RemoteTransportProvider>());

    public static void connect(final LocalNode node) {
        if (transportProviders.isEmpty())
            transportProviders.add(NonSharedMemoryTransportProvider.getInstance());

        for (final RemoteTransportProvider transportProvider : transportProviders) {
            node.getScheduler().execute(new Runnable(){
                public void run() {
                    transportProvider.connect(node);
                }
            });
        }
    }

    public static void disconnect(final LocalNode node) {
        if (transportProviders.isEmpty())
            transportProviders.add(NonSharedMemoryTransportProvider.getInstance());

        for (final RemoteTransportProvider transportProvider : transportProviders) {
            node.getScheduler().execute(new Runnable(){
                public void run() {
                    transportProvider.disconnect(node);
                }
            });
        }
    }
}
