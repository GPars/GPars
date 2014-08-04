package groovyx.gpars.dataflow.remote;

import groovyx.gpars.dataflow.*;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.message.RemoteDataflowVariableRequestMsg;
import groovyx.gpars.remote.netty.NettyClient;
import groovyx.gpars.remote.netty.NettyServer;
import groovyx.gpars.remote.netty.NettyTransportProvider;
import groovyx.gpars.serial.SerialMsg;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public final class RemoteDataflows extends LocalHost {
    /**
     * Stores DataflowVariables published in context of this instance of RemoteDataflows.
     */
    private final Map<String, DataflowVariable<?>> publishedVariables;

    /**
     * Stores promises to remote instances of DataflowVariables.
     */
    private final Map<String, DataflowVariable<?>> remoteVariables;

    private static Map<String, DataflowBroadcast> publishedBroadcasts = new ConcurrentHashMap<>();

    private static Map<String, DataflowVariable<RemoteDataflowBroadcast>> remoteBroadcasts = new ConcurrentHashMap<>();

    private static Map<String, DataflowQueue<?>> publishedQueues = new ConcurrentHashMap<>();

    private static Map<String, DataflowVariable<RemoteDataflowQueue<?>>> remoteQueues = new ConcurrentHashMap<>();

    private static LocalHost clientLocalHost = new LocalHost(); // TODO what about server?

    /**
     * Server for current instance of RemoteDataflows.
     */
    private NettyServer server;

    RemoteDataflows() {
        publishedVariables = new ConcurrentHashMap<>();
        remoteVariables = new ConcurrentHashMap<>();
    }

    /**
     * Publishes {@link groovyx.gpars.dataflow.DataflowVariable} under given name.
     * It overrides previously published variable if the same name is given.
     * @param variable the variable to be published
     * @param name the name under which variable is published
     */
    public void publish(DataflowVariable<?> variable, String name) {
        publishedVariables.put(name, variable);
    }

    /**
     * Retrieves {@link groovyx.gpars.dataflow.DataflowVariable} published under specified name on remote host.
     * @param host the address of remote host
     * @param port the the port of remote host
     * @param name the name under which variable was published
     * @return promise of {@link groovyx.gpars.dataflow.remote.RemoteDataflowVariable}
     */
    public Promise<DataflowVariable> getVariable(String host, int port, String name) {
        DataflowVariable remoteVariable = remoteVariables.get(name);
        if (remoteVariable == null) {
            DataflowVariable newRemoteVariable = new DataflowVariable<>();
            remoteVariable = remoteVariables.putIfAbsent(name, newRemoteVariable);
            if (remoteVariable == null) {
                createRequest(host, port, new RemoteDataflowVariableRequestMsg(this.getId(), name));
                remoteVariable = newRemoteVariable;
            }

        }
        return remoteVariable;
    }

    public static void publish(DataflowBroadcast broadcastStream, String name) {
        publishedBroadcasts.put(name, broadcastStream);
    }

    public static DataflowBroadcast getBroadcastStream(String name) {
        return publishedBroadcasts.get(name);

    }

    public static Future<DataflowReadChannel> getReadChannel(String host, int port, String name) {
        // TODO wrong use of concurent map
        // NettyTransportProvider.setRemoteBroadcastsRegistry(remoteBroadcasts);

        DataflowVariable<RemoteDataflowBroadcast> remoteStreamVariable = remoteBroadcasts.get(name);
        if (remoteStreamVariable == null) {
            remoteStreamVariable = new DataflowVariable<>();
            remoteBroadcasts.put(name, remoteStreamVariable);
            //NettyTransportProvider.getDataflowReadChannel(host, port, name);
        }

        return new RemoteDataflowReadChannelFuture(remoteStreamVariable);
    }

    public static DataflowQueue<?> getDataflowQueue(String name) {
        return publishedQueues.get(name);
    }

    public static void publish(DataflowQueue<?> queue, String name) {
        publishedQueues.put(name, queue);
    }

    public static Future<RemoteDataflowQueue<?>> getDataflowQueue(String host, int port, String name) {
        // TODO wrong use of concurent map
        // clientLocalHost.setRemoteDataflowQueueRegistry(remoteQueues);

        DataflowVariable<RemoteDataflowQueue<?>> remoteQueueVariable = remoteQueues.get(name);
        if (remoteQueueVariable == null) {
            remoteQueueVariable = new DataflowVariable<>();
            remoteQueues.put(name, remoteQueueVariable);
            //NettyTransportProvider.getDataflowQueue(host, port, name, clientLocalHost);
        }

        return new RemoteDataflowQueueFuture(remoteQueueVariable);
    }

    public static RemoteDataflows create() {
        return new RemoteDataflows();
    }

    public void startServer(String host, int port) {
        if (server != null) {
            throw new IllegalStateException("Server is already started");
        }

        server = NettyTransportProvider.createServer(host, port, this);
        server.start();
    }

    public void stopServer() {
        if (server == null) {
            throw new IllegalStateException("Server has not been started");
        }

        server.stop();
    }

    @Override
    public <T> void registerProxy(Class<T> klass, String name, T object) {
        System.err.println("register proxy");
        System.err.println(klass);
        System.err.println(name);
        System.err.println(object);
        // TODO
        if (klass == DataflowVariable.class) {
            DataflowVariable remoteVar = remoteVariables.get(name);
            remoteVar.bindUnique(object);
        }
    }

    @Override
    public <T> T get(Class<T> klass, String name) {
        System.err.println("get");
        System.err.println(klass);
        System.err.println(name);

        if (klass == DataflowVariable.class) {
            return klass.cast(publishedVariables.get(name));
        }

        return null;
    }

    private void createRequest(String host, int port, SerialMsg msg) {
        NettyClient client = NettyTransportProvider.createClient(host, port, this, connection -> {
            if (connection.getHost() != null)
                connection.write(msg);
        });
        client.start();
    }

    /*
        public static void getDataflowVariable(String host, int port, String name, LocalHost localHost) {
        NettyClient client = new NettyClient(localHost, host, port, connection -> {
            if (connection.getHost() != null)
                connection.write(new RemoteDataflowVariableRequestMsg(localHost.getId(), name));
        });
        client.start();
    }

    public static void getDataflowReadChannel(String host, int port, String name) {
        if (localHost == null) {
            localHost = new LocalHost();
        }

        NettyClient client = new NettyClient(localHost, host, port, connection -> {
            if (connection.getHost() != null)
                connection.write(new RemoteDataflowReadChannelRequestMsg(localHost.getId(), name));
        });
        client.start();
    }

    public static void setRemoteBroadcastsRegistry(Map<String, DataflowVariable<RemoteDataflowBroadcast>> remoteBroadcasts) {
        if (localHost == null) {
            localHost = new LocalHost();
        }

        if (localHost.getRemoteBroadcastsRegistry() == null) {
            localHost.setRemoteBroadcastsRegistry(remoteBroadcasts);
        }
    }

    public static void getDataflowQueue(String host, int port, String name, LocalHost localHost) {
        NettyClient client = new NettyClient(localHost, host, port, connection -> {
            if (connection.getHost() != null) {
                connection.write(new RemoteDataflowQueueRequestMsg(localHost.getId(), name));
            }
        });
        client.start();
    }
     */
}
