package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowChannel;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflowQueue;
import groovyx.gpars.dataflow.remote.RemoteDataflows;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteDataflowQueueValueRequestMsg<T> extends SerialMsg{
    private final DataflowVariable<T> value;
    private final DataflowChannel<?> queue;

    public RemoteDataflowQueueValueRequestMsg(RemoteDataflowQueue<T> queue, DataflowVariable<T> value) {
        this.queue = queue;
        this.value = value;
    }

    @Override
    public void execute(RemoteConnection conn) {
        try {
            value.bindUnique((T)queue.getVal());
        } catch (InterruptedException e) {
            value.bindError(e);
        }
    }
}
