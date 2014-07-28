package groovyx.gpars.remote.message;

import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

import java.util.UUID;

public class RemoteAgentWithRemoteExectionPolicyRequestMsg extends SerialMsg {
    public RemoteAgentWithRemoteExectionPolicyRequestMsg(UUID id, String name) {
        super(id);
    }

    @Override
    public void execute(RemoteConnection conn) {
        System.err.println("agent request");
    }
}
