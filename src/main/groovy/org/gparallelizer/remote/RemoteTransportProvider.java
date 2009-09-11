//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.remote;

import org.gparallelizer.actors.Actor;

import java.util.HashMap;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.ArrayList;

/**
 * Represents communication method with remote hosts
 *
 * @author Alex Tkachman
 */
public abstract class RemoteTransportProvider {
    /**
     * Unique id of the provider
     */
    private final UUID id = UUID.randomUUID();

    private final WeakHashMap<Actor,UUID> localActors = new WeakHashMap<Actor, UUID>();

    protected final HashMap<UUID,Actor> localActorsId = new HashMap<UUID, Actor>();

    /**
     * Registry of remote nodes known to the provider
     */
    protected final HashMap<UUID, RemoteNode> registry = new HashMap<UUID, RemoteNode>();

    /**
     * Connect local node to the provider
     * @param node local node
     */
    public void connect(final LocalNode node) {
        Actor mainActor = node.getMainActor();
        if (mainActor != null) {
            synchronized (localActors) {
                localActors.put(mainActor, node.getId());
                localActorsId.put(node.getId(), mainActor);
            }
        }
    }

    /**
     * Disconnect local node from the provider
     *
     * @param node local node
     */
    public void disconnect(final LocalNode node) {
        synchronized (localActors) {
            Actor actor = localActorsId.remove(node.getId());
            if (actor != null)
                localActors.remove(actor);
        }
    }

    public void disconnect () {
        LocalNodeRegistry.removeTransportProvider(this);
    }

    /**
     * Getter for provider id
     *
     * @return unique id
     */
    public UUID getId() {
        return id;
    }

    public UUID getLocalActorId (Actor actor) {
        if (actor == null) {
            return null;
        }

        synchronized (localActors) {
            UUID uid = localActors.get(actor);
            if (uid == null) {
                uid = UUID.randomUUID();
                localActors.put(actor, uid);
                localActorsId.put(uid, actor);
            }
            return uid;
        }
    }

    public Actor getLocalActor(UUID uuid) {
        synchronized (localActors) {
            return localActorsId.get(uuid);
        }
    }
}
