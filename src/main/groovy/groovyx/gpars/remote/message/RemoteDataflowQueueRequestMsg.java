package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.remote.RemoteDataflows;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

import java.util.UUID;


public class RemoteDataflowQueueRequestMsg extends SerialMsg {
    final String name;

    public RemoteDataflowQueueRequestMsg(UUID id, String name) {
        super(id);
        this.name = name;
    }

    @Override
    public void execute(RemoteConnection conn) {
        updateRemoteHost(conn);

        DataflowQueue<?> queue = RemoteDataflows.getDataflowQueue(name);
        conn.write(new RemoteDataflowQueueReplyMsg(name, queue));
    }
}
