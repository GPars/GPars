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

package groovyx.gpars.agent.remote;

import groovyx.gpars.agent.Agent;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.netty.NettyTransportProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Remoting context for Agents. Manages serialization, publishing and retrieval.
 *
 * @author Rafal Slawik
 */
public final class RemoteAgents {
    private RemoteAgents() {}

    private static final Map<String, Agent<?>> publishedAgents = new ConcurrentHashMap<>();

    private static final Map<String, DataflowVariable<RemoteAgent<?>>> remoteAgents = new ConcurrentHashMap<>();

    private static final LocalHost clientLocalHost = new LocalHost(); // TODO server localhost

    public static void publish(Agent<?> agent, String name) {
        publishedAgents.put(name, agent);
    }

    public static Agent<?> get(String name) {
        return publishedAgents.get(name);
    }

    public static Future<RemoteAgent<?>> get(String host, int port, String name) {
        // TODO wrong use of concurrent map
        clientLocalHost.setRemoteAgentsRegistry(remoteAgents);

        DataflowVariable<RemoteAgent<?>> agentVariable = remoteAgents.get(name);
        if (agentVariable == null) {
            agentVariable = new DataflowVariable<>();
            remoteAgents.put(name, agentVariable);
            NettyTransportProvider.getAgent(host, port, name, clientLocalHost);
        }
        return new RemoteAgentFuture(agentVariable);
    }
}
