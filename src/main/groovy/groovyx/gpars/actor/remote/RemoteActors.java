package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.remote.LocalNode;
import groovyx.gpars.remote.RemoteNode;
import groovyx.gpars.remote.RemoteNodeDiscoveryListener;
import groovyx.gpars.remote.netty.ClientNettyTransportProvider;
import groovyx.gpars.remote.netty.NettyTransportProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public final class RemoteActors {
    private static Map<Actor, NettyTransportProvider> providers = new HashMap<>();

    private RemoteActors() {}

    public static Actor get(String host, int port) {
        try {
            ClientNettyTransportProvider provider = new ClientNettyTransportProvider(host, port);
            CountDownLatch latch = new CountDownLatch(1);
            final Actor[] remoteActor = new Actor[1];
            LocalNode node = new LocalNode(provider, new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, null);
            // TODO fix connection setup
            node.addDiscoveryListener(new RemoteNodeDiscoveryListener() {
                @Override
                public void onConnect(RemoteNode node) {
                    super.onConnect(node);
                    remoteActor[0] = node.getMainActor();
                    latch.countDown();
                }
            });
            latch.await();
            return remoteActor[0];
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void register(Actor actor) {
        try {
            NettyTransportProvider provider = new NettyTransportProvider("localhost", 9000);
            providers.put(actor, provider);
            LocalNode node = new LocalNode(provider, null, actor);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        for (NettyTransportProvider provider : providers.values()) {
            try {
                provider.disconnect();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
