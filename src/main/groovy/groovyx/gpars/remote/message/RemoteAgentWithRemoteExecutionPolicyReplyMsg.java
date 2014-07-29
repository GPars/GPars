package groovyx.gpars.remote.message;

import groovyx.gpars.agent.Agent;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteAgentWithRemoteExecutionPolicyReplyMsg extends SerialMsg {
    private final String name;
    private final Agent<?> agent;

    public RemoteAgentWithRemoteExecutionPolicyReplyMsg(String name, Agent<?> agent) {
        this.name = name;
        this.agent = agent;
    }

    @Override
    public void execute(RemoteConnection conn) {
        DataflowVariable<Agent<?>> agentVariable = conn.getLocalHost().getRemoteAgentsRegistry().get(name);
        agentVariable.bindUnique(agent);
    }
}
