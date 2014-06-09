package groovyx.gpars.actor.remote;

import groovy.lang.Closure;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.LocalNode;
import groovyx.gpars.remote.RemoteNode;
import groovyx.gpars.remote.RemoteNodeDiscoveryListener;
import groovyx.gpars.remote.netty.ClientNettyTransportProvider;
import groovyx.gpars.remote.netty.NettyTransportProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public final class RemoteActors {

    private RemoteActors() {}

    private static List<ClientNettyTransportProvider> providers = new ArrayList<>();

    private static LocalHost localHost;

    public static Actor get(String host, int port, String actorName) {
        try {
            ClientNettyTransportProvider provider = new ClientNettyTransportProvider(host, port);
            providers.add(provider);
            Actor remoteActor = provider.connect();
            return remoteActor;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void register(Actor actor, String name) {
        if (localHost == null) {
            localHost = new LocalHost();
        }

        try {
            NettyTransportProvider provider = new NettyTransportProvider("localhost", 9000);
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
            provider.disconnect();
            return null;
        }
    }

    public static void shutdown() {
        providers.stream().forEach(ClientNettyTransportProvider::disconnect);
    }
}
