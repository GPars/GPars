package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public final class RemoteActors {
    private RemoteActors() {}

    public static Actor get(String host, int port) {
        throw new NotImplementedException();
    }

    public static void register(Actor actor) {
        throw new NotImplementedException();
    }
}
