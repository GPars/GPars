package groovyx.gpars.remote.message;

import groovyx.gpars.agent.Agent;
import groovyx.gpars.agent.remote.RemoteAgents;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

import java.util.UUID;

public class RemoteAgentWithRemoteExecutionPolicyRequestMsg extends SerialMsg {
    private final String name;

    public RemoteAgentWithRemoteExecutionPolicyRequestMsg(UUID id, String name) {
        super(id);
        this.name = name;
    }

    @Override
    public void execute(RemoteConnection conn) {
        updateRemoteHost(conn);

        Agent<?> agent = RemoteAgents.get(name);
        conn.write(new RemoteAgentWithRemoteExecutionPolicyReplyMsg(name, agent));
    }
}
