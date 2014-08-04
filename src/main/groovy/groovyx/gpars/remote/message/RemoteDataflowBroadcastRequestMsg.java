package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowBroadcast;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

import java.util.UUID;

public class RemoteDataflowBroadcastRequestMsg extends SerialMsg {
    final String name;

    public RemoteDataflowBroadcastRequestMsg(UUID hostId, String name) {
        super(hostId);
        this.name = name;
    }

    @Override
    public void execute(RemoteConnection conn) {
        updateRemoteHost(conn);

        DataflowBroadcast stream = conn.getLocalHost().get(DataflowBroadcast.class, name);
        conn.write(new RemoteDataflowBroadcastReplyMsg(name, stream));
    }
}
