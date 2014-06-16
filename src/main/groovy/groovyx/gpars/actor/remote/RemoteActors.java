package groovyx.gpars.actor.remote;

import groovy.lang.Closure;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.LocalNode;
import groovyx.gpars.remote.RemoteNode;
import groovyx.gpars.remote.RemoteNodeDiscoveryListener;
import groovyx.gpars.remote.netty.ClientNettyTransportProvider;
import groovyx.gpars.remote.netty.NettyTransportProvider;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public final class RemoteActors {
    private RemoteActors() {}

//    private static List<ClientNettyTransportProvider> providers = new ArrayList<>();

//    public static Actor get(String host, int port, String actorName) {
//        try {
//            ClientNettyTransportProvider provider = new ClientNettyTransportProvider(host, port);
//            providers.add(provider);
//            Actor remoteActor = provider.register();
//            return remoteActor;
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public static Future<Actor> get(String host, int port, String actorName) {
        throw new UnsupportedOperationException();
    }

    public static void register(Actor actor, String name) {
        NettyTransportProvider.register(actor, name);
    }

//    private static class StopProviderClosure extends Closure<Void> {
//
//        private final NettyTransportProvider provider;
//
//        public StopProviderClosure(Object owner, NettyTransportProvider provider) {
//            super(owner);
//            this.provider = provider;
//        }
//
//        @Override
//        public Void call(Object... args) {
//            provider.disconnect();
//            return null;
//        }
//    }

//    public static void shutdown() {
//        providers.stream().forEach(ClientNettyTransportProvider::disconnect);
//    }
}
