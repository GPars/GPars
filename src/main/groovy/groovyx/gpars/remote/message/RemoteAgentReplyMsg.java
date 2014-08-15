package groovyx.gpars.remote.message;

import groovyx.gpars.agent.Agent;
import groovyx.gpars.agent.AgentCore;
import groovyx.gpars.agent.remote.RemoteAgent;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteAgentReplyMsg extends SerialMsg {
    private final String name;
    private final AgentCore agent;

    public RemoteAgentReplyMsg(String name, Agent<?> agent) {
        this.name = name;
        this.agent = agent;
    }

    @Override
    public void execute(RemoteConnection conn) {
        updateRemoteHost(conn);
        conn.getLocalHost().registerProxy(RemoteAgent.class, name, ((RemoteAgent) agent));
    }
}
