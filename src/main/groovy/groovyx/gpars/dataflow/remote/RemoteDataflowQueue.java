package groovyx.gpars.dataflow.remote;


import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.serial.RemoteSerialized;

public class RemoteDataflowQueue implements RemoteSerialized {
    public RemoteDataflowQueue(RemoteHost host) {
        System.err.println("RemoteDataflowQueue");
    }
}
