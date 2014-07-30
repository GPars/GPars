package groovyx.gpars.agent.remote;

import groovy.lang.Closure;
import groovyx.gpars.agent.Agent;
import groovyx.gpars.agent.AgentCore;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.remote.message.RemoteAgentGetValMsg;
import groovyx.gpars.remote.message.RemoteAgentSendMessage;
import groovyx.gpars.serial.RemoteSerialized;

public class RemoteAgent<T> extends AgentCore implements RemoteSerialized{
    private final RemoteHost remoteHost;

    private AgentClosureExecutionPolicy executionPolicy;

    public RemoteAgent(RemoteHost remoteHost) {
        this.remoteHost = remoteHost;
    }

    @Override
    public void handleMessage(Object message) {
        remoteHost.write(new RemoteAgentSendMessage(this, message));
    }

    public T getVal() throws InterruptedException {
        DataflowVariable<T> valueVariable = new DataflowVariable<>();
        remoteHost.write(new RemoteAgentGetValMsg(this, valueVariable));
        return valueVariable.getVal();
    }

}
