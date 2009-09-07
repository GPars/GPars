package org.gparallelizer.remote;

import org.gparallelizer.remote.sharedmemory.SharedMemoryTransportProvider;

import java.util.*;

public class LocalNodeRegistry {
    private static final Set<RemoteTransportProvider> transportProviders
            = Collections.synchronizedSet(new HashSet<RemoteTransportProvider>(Arrays.asList(SharedMemoryTransportProvider.getInstance())));

    public static void connect(final LocalNode node) {
        for (final RemoteTransportProvider transportProvider : transportProviders) {
            node.getScheduler().execute(new Runnable(){
                public void run() {
                    transportProvider.connect(node);
                }
            });
        }
    }

    public static void disconnect(final LocalNode node) {
        for (final RemoteTransportProvider transportProvider : transportProviders) {
            node.getScheduler().execute(new Runnable(){
                public void run() {
                    transportProvider.disconnect(node);
                }
            });
        }
    }
}
