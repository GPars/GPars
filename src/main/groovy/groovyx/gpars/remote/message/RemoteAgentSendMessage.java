package groovyx.gpars.remote.message;

import groovy.lang.Closure;
import groovyx.gpars.agent.AgentCore;
import groovyx.gpars.agent.remote.RemoteAgent;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteAgentSendMessage extends SerialMsg {
    private final AgentCore agent;
    private final Object message;

    public RemoteAgentSendMessage(RemoteAgent agent, Object message) {
        this.message = prepareMessage(message);
        this.agent = agent;
    }

    @Override
    public void execute(RemoteConnection conn) {
        agent.send(message);
    }

    private Object prepareMessage(Object message) {
        if (message instanceof Closure) {
            return ((Closure) message).dehydrate();
        }
        return message;
    }
}
