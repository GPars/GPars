package groovyx.gpars.remote.message;

import groovy.lang.Closure;
import groovyx.gpars.agent.AgentCore;
import groovyx.gpars.agent.remote.AgentClosureExecutionClosure;
import groovyx.gpars.agent.remote.RemoteAgent;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;


public class RemoteAgentSendClosureMessage<T> extends SerialMsg {
    private final AgentCore agent;
    private final DataflowVariable<T> oldValueVariable;
    private final DataflowVariable<T> newValueVariable;

    public RemoteAgentSendClosureMessage(RemoteAgent<T> agent, DataflowVariable<T> oldValue, DataflowVariable<T> newValue) {
        this.agent = agent;
        this.oldValueVariable = oldValue;
        this.newValueVariable = newValue;
    }

    @Override
    public void execute(RemoteConnection conn) {
        agent.send(new AgentClosureExecutionClosure(agent, oldValueVariable, newValueVariable));
    }
}
