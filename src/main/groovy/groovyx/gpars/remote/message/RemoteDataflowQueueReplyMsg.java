package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowChannel;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflowQueue;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteDataflowQueueReplyMsg extends SerialMsg {
    final String name;
    final DataflowChannel<?> queue;

    public RemoteDataflowQueueReplyMsg(String name, DataflowQueue<?> queue) {
        this.name = name;
        this.queue = queue;
    }

    @Override
    public void execute(RemoteConnection conn) {
        DataflowVariable<RemoteDataflowQueue<?>> remoteQueueVariable = conn.getLocalHost().getRemoteDataflowQueueRegistry().get(name);
        RemoteDataflowQueue<?> remoteQueue = (RemoteDataflowQueue)queue;
        remoteQueueVariable.bindUnique(remoteQueue);
    }
}
