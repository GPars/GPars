package groovyx.gpars.agent.remote;

import groovy.lang.Closure;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.message.RemoteAgentGetValMsg;
import groovyx.gpars.remote.message.RemoteAgentSendMessage;
import groovyx.gpars.serial.SerialMsg;

public enum AgentClosureExecutionPolicy {
    LOCAL {
        @Override
        public SerialMsg prepareMessage(RemoteAgent<?> agent, Object message) {
            if (message instanceof Closure) {
                Closure closure = (Closure) message;
                DataflowVariable<?> oldValue = new DataflowVariable<>();
                DataflowVariable<?> newValue = new DataflowVariable<>();
                return null;
            }
            return new RemoteAgentSendMessage(agent, message);
        }
    },
    REMOTE {
        @Override
        public SerialMsg prepareMessage(RemoteAgent<?> agent, Object message) {
            return new RemoteAgentSendMessage(agent, message);
        }
    };

    public abstract SerialMsg prepareMessage(RemoteAgent<?> agent, Object message);

    public <T> SerialMsg prepareGetValMessage(RemoteAgent<T> agent, DataflowVariable<T> resultVariable) {
        return new RemoteAgentGetValMsg<>(agent, resultVariable);
    }
}
