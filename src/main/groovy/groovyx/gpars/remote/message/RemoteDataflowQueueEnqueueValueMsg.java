package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowChannel;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.remote.RemoteDataflowQueue;
import groovyx.gpars.dataflow.remote.RemoteDataflows;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteDataflowQueueEnqueueValueMsg<T> extends SerialMsg {
    private final DataflowChannel<T> queue;
    private final T value;

    public RemoteDataflowQueueEnqueueValueMsg(RemoteDataflowQueue<T> queue, T value) {
        this.queue = queue;
        this.value = value;
    }

    @Override
    public void execute(RemoteConnection conn) {
        queue.bind(value);
    }
}
