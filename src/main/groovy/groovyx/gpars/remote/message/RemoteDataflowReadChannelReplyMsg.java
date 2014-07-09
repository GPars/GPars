package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

import java.util.UUID;

public class RemoteDataflowReadChannelReplyMsg extends SerialMsg {
    private final String name;
    private final DataflowReadChannel channel;

    public RemoteDataflowReadChannelReplyMsg(String name, DataflowReadChannel channel) {
        this.name = name;
        this.channel = channel;
    }

    @Override
    public void execute(RemoteConnection conn) {
        System.err.println("remote dataflow read channel reply");
    }
}
