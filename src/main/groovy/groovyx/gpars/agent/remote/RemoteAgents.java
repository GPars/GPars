package groovyx.gpars.agent.remote;

import groovyx.gpars.agent.Agent;
import groovyx.gpars.dataflow.DataflowVariable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public final class RemoteAgents {
    private RemoteAgents() {}

    private static final Map<String, Agent<?>> publishedAgents = new ConcurrentHashMap<>();

    private static final Map<String, DataflowVariable<Agent<?>>> remoteAgents = new ConcurrentHashMap<>();

    public static void publish(Agent<?> agent, String name) {
        publishedAgents.put(name, agent);
    }

    public static Agent<?> get(String name) {
        return publishedAgents.get(name);
    }

    public static Future<Agent<?>> get(String host, int port, String name, ClojureExecutionPolicy policy) {
        // TODO wrong use of concurrent map

        DataflowVariable<Agent<?>> agentVariable = remoteAgents.get(name);
        if (agentVariable == null) {
            agentVariable = new DataflowVariable<>();
            remoteAgents.put(name, agentVariable);
        }
        return new RemoteAgentFuture(agentVariable);
    }
}
