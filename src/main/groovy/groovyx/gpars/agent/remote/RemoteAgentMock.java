package groovyx.gpars.agent.remote;

public class RemoteAgentMock {
    private Object state = null;

    public void updateValue(Object newValue) {
        state = newValue;
    }

    public Object getState() {
        return state;
    }
}
