package groovyx.gpars.dataflow.remote;

import groovyx.gpars.dataflow.DataflowBroadcast;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.stream.DataflowStreamWriteAdapter;
import groovyx.gpars.remote.netty.NettyTransportProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public final class RemoteDataflows {
    private static Map<String, DataflowVariable<?>> publishedVariables = new ConcurrentHashMap<>();

    private static Map<String, DataflowVariable<?>> remoteVariables = new ConcurrentHashMap<>();

    private static Map<String, DataflowBroadcast> publishedBroadcasts = new ConcurrentHashMap<>();

    private static Map<String, DataflowVariable<RemoteDataflowBroadcast>> remoteBroadcasts = new ConcurrentHashMap<>();

    private static Map<String, DataflowQueue<?>> publishedQueues = new ConcurrentHashMap<>();

    private static Map<String, DataflowVariable<DataflowQueue<?>>> remoteQueues = new ConcurrentHashMap<>();

    private RemoteDataflows() {}

    /**
     * Publishes {@link groovyx.gpars.dataflow.DataflowVariable} under chosen name.
     * @param variable the variable to be published
     * @param name the name under which variable is published
     * @param <T> type of variable
     */
    public static <T> void publish(DataflowVariable<T> variable, String name) {
        publishedVariables.put(name, variable);
    }

    /**
     * Retrieves {@link groovyx.gpars.dataflow.DataflowVariable} published under specified name (locally).
     * @param name the name under which variable was published
     * @return the variable registered under specified name or <code>null</code> if none variable is registered under that name
     */
    public static DataflowVariable<?> get(String name) {
        return publishedVariables.get(name);
    }

    /**
     * Retrieves {@link groovyx.gpars.dataflow.DataflowVariable} published under specified name on remote host.
     * @param host the address of remote host
     * @param port the the port of remote host
     * @param name the name under which variable was published
     * @return future of {@link groovyx.gpars.dataflow.remote.RemoteDataflowVariable}
     * @see groovyx.gpars.dataflow.remote.RemoteDataflowVariableFuture
     */
    public static Future<DataflowVariable> get(String host, int port, String name) {
        NettyTransportProvider.setRemoteDataflowsRegistry(remoteVariables);

        DataflowVariable remoteVariable = remoteVariables.get(name);
        if (remoteVariable == null) {
            remoteVariable = new DataflowVariable<>();
            remoteVariables.put(name, remoteVariable);
            NettyTransportProvider.getDataflowVariable(host, port, name);
        }
        return new RemoteDataflowVariableFuture(remoteVariable);
    }

    public static void publish(DataflowBroadcast broadcastStream, String name) {
        publishedBroadcasts.put(name, broadcastStream);
    }

    public static DataflowBroadcast getBroadcastStream(String name) {
        return publishedBroadcasts.get(name);

    }

    public static Future<DataflowReadChannel> getReadChannel(String host, int port, String name) {
        NettyTransportProvider.setRemoteBroadcastsRegistry(remoteBroadcasts);

        DataflowVariable<RemoteDataflowBroadcast> remoteStreamVariable = remoteBroadcasts.get(name);
        if (remoteStreamVariable == null) {
            remoteStreamVariable = new DataflowVariable<>();
            remoteBroadcasts.put(name, remoteStreamVariable);
            NettyTransportProvider.getDataflowReadChannel(host, port, name);
        }

        return new RemoteDataflowReadChannelFuture(remoteStreamVariable);
    }

    public static DataflowQueue<?> getDataflowQueue(String name) {
        return publishedQueues.get(name);
    }

    public static void publish(DataflowQueue<?> queue, String name) {
        publishedQueues.put(name, queue);
    }

    public static Future<DataflowQueue<?>> getDataflowQueue(String host, int port, String name) {
        return new RemoteDataflowQueueFuture(null);
    }
}
