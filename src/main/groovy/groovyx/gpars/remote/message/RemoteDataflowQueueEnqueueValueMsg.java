package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.remote.RemoteDataflows;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteDataflowQueueEnqueueValueMsg<T> extends SerialMsg {
    final String queueName;
    final T value;

    public RemoteDataflowQueueEnqueueValueMsg(String queueName, T value) {
        this.queueName = queueName;
        this.value = value;
    }

    @Override
    public void execute(RemoteConnection conn) {
        DataflowQueue<T> queue = (DataflowQueue<T>) RemoteDataflows.getDataflowQueue(queueName);
        queue.bind(value);
    }
}
