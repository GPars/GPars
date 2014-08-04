package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.remote.netty.NettyTransportProvider;

import java.util.concurrent.Future;

public final class RemoteActors {
    private RemoteActors() {}

    public static Future<Actor> get(String host, int port, String name) {
        return NettyTransportProvider.get(host, port, name);
    }

    public static void register(Actor actor, String name) {
        NettyTransportProvider.register(actor, name);
    }
}
