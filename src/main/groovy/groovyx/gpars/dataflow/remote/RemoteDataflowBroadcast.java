package groovyx.gpars.dataflow.remote;


import groovyx.gpars.dataflow.DataflowBroadcast;
import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.stream.DataflowStream;
import groovyx.gpars.dataflow.stream.DataflowStreamReadAdapter;
import groovyx.gpars.dataflow.stream.DataflowStreamWriteAdapter;
import groovyx.gpars.remote.RemoteHost;

public class RemoteDataflowBroadcast<T> extends DataflowStreamWriteAdapter<T> {
    public RemoteDataflowBroadcast(RemoteHost remoteHost) {
        super(new DataflowStream<T>());
    }

    public synchronized String toString() {
        return "RemoteDataflowBroadcast for " + super.toString();
    }

    public DataflowReadChannel<T> createReadChannel() {
        return new DataflowStreamReadAdapter<T>(getHead());
    }
}
