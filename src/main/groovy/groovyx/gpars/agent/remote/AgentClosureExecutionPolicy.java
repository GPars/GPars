package groovyx.gpars.agent.remote;

import groovy.lang.Closure;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.message.RemoteAgentGetValMsg;
import groovyx.gpars.remote.message.RemoteAgentSendClosureMessage;
import groovyx.gpars.remote.message.RemoteAgentSendMessage;
import groovyx.gpars.serial.SerialMsg;

public enum AgentClosureExecutionPolicy {
    LOCAL {
        @Override
        public SerialMsg prepareMessage(RemoteAgent<?> agent, Object message) {
            if (message instanceof Closure) {
                Closure closure = (Closure) message;
                RemoteAgentMock mock = new RemoteAgentMock();
                closure.setDelegate(mock);
                DataflowVariable<?> oldValue = new DataflowVariable<>();
                DataflowVariable newValue = new DataflowVariable();
                oldValue.whenBound(new MessageStream() {
                    @Override
                    public MessageStream send(Object message) {
                        closure.call(message);
                        newValue.bindUnique(mock.getState());
                        return this;
                    }
                });
                return new RemoteAgentSendClosureMessage(agent, oldValue, newValue);
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
