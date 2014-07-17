package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflows;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteDataflowQueueValueRequestMsg<T> extends SerialMsg{
    final String queueName;
    final DataflowVariable<T> value;

    public RemoteDataflowQueueValueRequestMsg(String name, DataflowVariable<T> value) {
        this.queueName = name;
        this.value = value;
    }

    @Override
    public void execute(RemoteConnection conn) {
        DataflowQueue<?> queue = RemoteDataflows.getDataflowQueue(queueName);
        try {
            value.bindUnique((T)queue.getVal());
        } catch (InterruptedException e) {
            value.bindError(e);
        }
    }
}
