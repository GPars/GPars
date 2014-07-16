package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteDataflowQueueReplyMsg extends SerialMsg {
    final String name;
    final DataflowQueue<?> queue;

    public RemoteDataflowQueueReplyMsg(String name, DataflowQueue<?> queue) {
        this.name = name;
        this.queue = queue;
    }

    @Override
    public void execute(RemoteConnection conn) {
        DataflowVariable<DataflowQueue<?>> remoteQueueVariable = conn.getLocalHost().getRemoteDataflowQueueRegistry().get(name);
        remoteQueueVariable.bindUnique(queue);
    }
}
