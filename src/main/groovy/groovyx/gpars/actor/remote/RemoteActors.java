package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.netty.NettyServer;
import groovyx.gpars.remote.netty.NettyTransportProvider;

import java.util.concurrent.Future;

public final class RemoteActors extends LocalHost {

    private RemoteActors() {}

    public static Future<Actor> get(String host, int port, String name) {
        return NettyTransportProvider.get(host, port, name);
    }

    public static void register(Actor actor, String name) {
        NettyTransportProvider.register(actor, name);
    }

    public static RemoteActors create() {
        return new RemoteActors();
    }

    @Override
    public <T> void registerProxy(Class<T> klass, String name, T object) {

    }

    @Override
    public <T> T get(Class<T> klass, String name) {
        return null;
    }
}
