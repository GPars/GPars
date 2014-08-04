package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.Promise;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.message.RemoteActorRequestMsg;
import groovyx.gpars.remote.netty.NettyServer;
import groovyx.gpars.remote.netty.NettyTransportProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public final class RemoteActors extends LocalHost {

    /**
     * Stores Actors published in context of this instance of RemoteActors.
     */
    private final Map<String, Actor> publishedActors;

    /**
     * Stores promises to remote instances of Actors.
     */
    private final Map<String, DataflowVariable<Actor>> remoteActors;

    private RemoteActors() {
        publishedActors = new ConcurrentHashMap<>();
        remoteActors = new ConcurrentHashMap<>();
    }

    /**
     * Publishes {@link groovyx.gpars.actor.Actor} under given name.
     * It overrides previously published Actor if the same name is given.
     * @param actor the Actor to be published
     * @param name the name under which Actor is published
     */
    public void publish(Actor actor, String name) {
        publishedActors.put(name, actor);
    }

    /**
     * Retrieves {@link groovyx.gpars.actor.Actor} published under specified name on remote host.
     * @param host the address of remote host
     * @param port the port of remote host
     * @param name the name under which Actor was published
     * @return promise of {@link groovyx.gpars.actor.remote.RemoteActor}
     */
    public Promise<Actor> get(String host, int port, String name) {
        return getPromise(remoteActors, name, host, port, new RemoteActorRequestMsg(this.getId(), name));
    }

    public static RemoteActors create() {
        return new RemoteActors();
    }

    @Override
    public <T> void registerProxy(Class<T> klass, String name, T object) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public <T> T get(Class<T> klass, String name) {
        throw new UnsupportedOperationException("TODO");
    }
}
