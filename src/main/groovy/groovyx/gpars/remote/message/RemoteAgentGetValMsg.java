package groovyx.gpars.remote.message;

import groovyx.gpars.agent.Agent;
import groovyx.gpars.agent.AgentCore;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteAgentGetValMsg<T> extends SerialMsg {
    private final AgentCore agent;
    private final DataflowVariable<T> valueVariable;

    public RemoteAgentGetValMsg(AgentCore agent, DataflowVariable<T> valueVariable) {
        this.agent = agent;
        this.valueVariable = valueVariable;
    }

    @Override
    public void execute(RemoteConnection conn) {
        try {
            T value = ((Agent<T>) agent).getVal();
            valueVariable.bindUnique(value);
        } catch (InterruptedException e) {
            valueVariable.bindError(e);
        }
    }
}
