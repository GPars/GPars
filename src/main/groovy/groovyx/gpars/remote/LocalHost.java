// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2010, 2013  The original author or authors
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

package groovyx.gpars.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.remote.RemoteActorFuture;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflowBroadcast;
import groovyx.gpars.dataflow.remote.RemoteDataflowQueue;
import groovyx.gpars.dataflow.remote.RemoteDataflowVariable;
import groovyx.gpars.serial.SerialContext;
import groovyx.gpars.serial.SerialHandles;

import java.util.*;

/**
 * Represents communication point with other local hosts.
 * Usually it is enough to have one LocalHost per JVM but it is possible to have several.
 * <p>
 * It can be one or several local nodes hosted on local host. For most applications one should be enough
 * but sometimes several can be useful as well.
 * </p>
 * <p>
 * Local host contains
 * </p>
 * <ul>
 *   <li>remote hosts connected with this one</li>
 *   <li>local actors available on this host</li>
 * </ul>
 *
 * @author Alex Tkachman
 */
public class LocalHost extends SerialHandles {
    /**
     * Hosts known to the provider
     */
    protected final Map<UUID, RemoteHost> remoteHosts = new HashMap<UUID, RemoteHost>();

    protected final Map<String, Actor> localActors = new HashMap<>();

    protected final Map<String, Actor> remoteActors = new HashMap<>();

    private Map<String, List<DataflowVariable<Actor>>> remoteActorFutures = new HashMap<>();

    private Map<String, DataflowVariable<?>> remoteDataflows;
    private Map<String, DataflowVariable<RemoteDataflowBroadcast>> remoteBroadcastsRegistry;
    private Map<String, DataflowVariable<RemoteDataflowQueue<?>>> remoteDataflowQueueRegistry;

    /**
     * Registers actor under specific name
     * @param name
     * @param actor
     */
    public void register(String name, final Actor actor) {
        synchronized (localActors) {
            localActors.put(name, actor);
        }
    }

    public void disconnect() {
        synchronized (remoteHosts) {
            final Iterable<RemoteHost> copy = new ArrayList<RemoteHost>(remoteHosts.values());
            remoteHosts.clear();
            for (final RemoteHost remoteHost : copy) {
                remoteHost.disconnect();
            }
        }
    }

    @Override
    public SerialContext getSerialHost(final UUID hostId, final Object attachment) {
        final RemoteConnection connection = (RemoteConnection) attachment;
        synchronized (remoteHosts) {
            RemoteHost host = remoteHosts.get(hostId);
            if (host == null) {
                host = new RemoteHost(this, hostId);
                remoteHosts.put(hostId, host);
            }
            if (connection != null) {
                connection.setHost(host);
                host.addConnection(connection);
            }
            return host;
        }
    }

    public Actor getActor(String name) {
        return localActors.get(name);
    }

    public void connectRemoteNode(final UUID nodeId, final SerialContext host, final Actor mainActor) {
//        RemoteNode node;
//        synchronized (remoteNodes) {
//            node = remoteNodes.get(nodeId);
//            if (node == null) {
//                node = new RemoteNode(nodeId, host, mainActor);
//                remoteNodes.put(nodeId, node);
//            }
//        }
//
//        synchronized (localNodes) {
//            for (final LocalNode localNode : localNodes.values()) {
//                localNode.onConnect(node);
//            }
//        }
    }

    public void disconnectRemoteNode(final UUID nodeId) {
//        final RemoteNode node;
//        synchronized (remoteNodes) {
//            node = remoteNodes.remove(nodeId);
//        }
//
//        if (node != null) {
//            synchronized (localNodes) {
//                onDisconnectForLocalNodes(node);
//            }
//        }
    }

    public void onDisconnect(final SerialContext host) {
//        final Collection<RemoteNode> toRemove = new ArrayList<RemoteNode>();
//        synchronized (remoteNodes) {
//            for (final RemoteNode t : remoteNodes.values()) {
//                if (t.getRemoteHost() == host) {
//                    toRemove.add(t);
//                }
//            }
//            for (final RemoteNode t : toRemove) {
//                remoteNodes.remove(t.getId());
//            }
//        }
//
//        synchronized (localNodes) {  //todo consider moving the synchronized block inside the onDisconnectForLocalNodes() method
//            for (final RemoteNode t : toRemove) {
//                onDisconnectForLocalNodes(t);
//            }
//        }
    }

    public void registerRemote(String name, Actor actor) {
        synchronized (remoteActors) {
            remoteActors.put(name, actor);
        }
        synchronized (remoteActorFutures) {
            List<DataflowVariable<Actor>> futures = remoteActorFutures.get(name);
            if (futures != null) {
                futures.stream().forEach(var -> var.bindUnique(actor));
            }
            remoteActorFutures.remove(name);
        }
    }

    public void addRemoteActorFuture(String name, DataflowVariable<Actor> var) {
        synchronized (remoteActorFutures) {
            List<DataflowVariable<Actor>> futures = remoteActorFutures.get(name);
            if (futures == null) {
                futures = new ArrayList<>();
                futures.add(var);
                remoteActorFutures.put(name, futures);
            } else {
                futures.add(var);
            }
        }
    }

    public void setRemoteDataflowsRegistry(Map<String, DataflowVariable<?>> registry) {
        if (remoteDataflows != null) {
            throw new IllegalStateException("Remote dataflows registry is already set");
        }
        remoteDataflows = registry;
    }

    public Map<String, DataflowVariable<?>> getRemoteDataflowsRegistry() {
        return remoteDataflows;
    }

    public Map<String, DataflowVariable<RemoteDataflowBroadcast>> getRemoteBroadcastsRegistry() {
        return remoteBroadcastsRegistry;
    }

    public void setRemoteBroadcastsRegistry(Map<String, DataflowVariable<RemoteDataflowBroadcast>> remoteBroadcastsRegistry) {
        this.remoteBroadcastsRegistry = remoteBroadcastsRegistry;
    }

    public Map<String, DataflowVariable<RemoteDataflowQueue<?>>> getRemoteDataflowQueueRegistry() {
        return remoteDataflowQueueRegistry;
    }

    public void setRemoteDataflowQueueRegistry(Map<String, DataflowVariable<RemoteDataflowQueue<?>>> remoteDataflowQueueRegistry) {
        this.remoteDataflowQueueRegistry = remoteDataflowQueueRegistry;
    }
}
