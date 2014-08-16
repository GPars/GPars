// GPars - Groovy Parallel Systems
//
// Copyright © 2014  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.Promise;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.message.RemoteActorRequestMsg;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remoting context for Actors. Manages serialization, publishing and retrieval.
 *
 * @author Rafal Slawik
 */
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
        if (klass == RemoteActor.class) {
            remoteActors.get(name).bind(((Actor) object));
            return;
        }
        throw new IllegalArgumentException("Unsupported proxy type");
    }

    @Override
    public <T> T get(Class<T> klass, String name) {
        if (klass == Actor.class) {
            return klass.cast(publishedActors.get(name));
        }
        throw new IllegalArgumentException("Unsupported type");
    }
}
