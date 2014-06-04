package groovyx.gpars.actor.remote;

import groovy.lang.Closure;
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
            remoteActor[0].onStop(new StopClientProviderClosure(null, provider));
            return remoteActor[0];
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void register(Actor actor) {
        try {
            NettyTransportProvider provider = new NettyTransportProvider("10.0.0.1", 9000);
            provider.connect(actor);
            actor.onStop(new StopProviderClosure(null, provider));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class StopProviderClosure extends Closure<Void> {

        private final NettyTransportProvider provider;

        public StopProviderClosure(Object owner, NettyTransportProvider provider) {
            super(owner);
            this.provider = provider;
        }

        @Override
        public Void call(Object... args) {
            try {
                provider.disconnect();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private static class StopClientProviderClosure extends Closure<Void> {

        private final ClientNettyTransportProvider provider;

        public StopClientProviderClosure(Object owner, ClientNettyTransportProvider provider) {
            super(owner);
            this.provider = provider;
        }

        @Override
        public Void call(Object... args) {
            try {
                provider.disconnect();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
