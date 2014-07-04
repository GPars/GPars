package groovyx.gpars.dataflow.remote;

import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.remote.netty.NettyTransportProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public final class RemoteDataflows {
    private static Map<String, DataflowVariable<?>> publishedVariables = new ConcurrentHashMap<>();

    private static Map<String, RemoteDataflowVariableFuture> remoteVariablesFutures = new ConcurrentHashMap<>();

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
        RemoteDataflowVariableFuture future = remoteVariablesFutures.get(name);
        if (future == null) {
            DataflowVariable<DataflowVariable> remoteVariable = new DataflowVariable<>();
            future = new RemoteDataflowVariableFuture(remoteVariable);
            remoteVariablesFutures.put(name, future);
            NettyTransportProvider.getDataflowVariable(host, port, name);
        }
        return future;
    }
}
