package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflows;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

import java.util.UUID;

public class RemoteDataflowVariableRequestMsg extends SerialMsg {

    private final String name;

    public RemoteDataflowVariableRequestMsg(UUID hostId, String name) {
        super(hostId);
        this.name = name;
    }

    @Override
    public void execute(RemoteConnection conn) {
        updateRemoteHost(conn);

        DataflowVariable<?> var = RemoteDataflows.get(name);
        conn.write(new RemoteDataflowVariableReplyMsg(name, var));
    }

}
