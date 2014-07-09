package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.remote.RemoteDataflows;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

import java.util.UUID;

public class RemoteDataflowReadChannelRequestMsg extends SerialMsg {
    final String name;

    public RemoteDataflowReadChannelRequestMsg(UUID hostId, String name) {
        super(hostId);
        this.name = name;
    }

    @Override
    public void execute(RemoteConnection conn) {
        updateRemoteHost(conn);

        DataflowReadChannel channel = RemoteDataflows.getReadChannel(name);
        conn.write(new RemoteDataflowReadChannelReplyMsg(name, channel));
    }
}
