package groovyx.gpars.dataflow.remote;

import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.dataflow.*;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.message.RemoteDataflowBroadcastRequestMsg;
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
    private final Map<String, DataflowVariable<DataflowVariable>> remoteVariables;

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
     * @return promise of {@link groovyx.gpars.dataflow.DataflowVariable}
     */
    public Promise<DataflowVariable> getVariable(String host, int port, String name) {
        return getPromise(remoteVariables, name, host, port, new RemoteDataflowVariableRequestMsg(this.getId(), name));
    }

    /**
     * Publishes {@link groovyx.gpars.dataflow.DataflowBroadcast} under given name.
     * It overrides previously published broadcast if the same name is given.
     * @param broadcastStream the stream to be published
     * @param name the name under which stream is published
     */
    public void publish(DataflowBroadcast broadcastStream, String name) {
        publishedBroadcasts.put(name, broadcastStream);
    }

    /**
     * Retrieves {@link groovyx.gpars.dataflow.DataflowReadChannel} corresponding to
     * {@link groovyx.gpars.dataflow.DataflowBroadcast} published under given name on remote host.
     * @param host the address of remote host
     * @param port the port of remote host
     * @param name the name under which broadcast is published
     * @return promise of {@link groovyx.gpars.dataflow.DataflowReadChannel}
     */
    public Promise<DataflowReadChannel> getReadChannel(String host, int port, String name) {
        DataflowVariable<RemoteDataflowBroadcast> broadcastPromise = getPromise(remoteBroadcasts, name, host, port, new RemoteDataflowBroadcastRequestMsg(this.getId(), name));
        DataflowVariable<DataflowReadChannel> promise = new DataflowVariable<>();
        broadcastPromise.whenBound(new MessageStream() {
            @Override
            public MessageStream send(Object message) {
                promise.bind(((RemoteDataflowBroadcast) message).createReadChannel());
                return this;
            }
        });
        return promise;
    }

    // -- todo

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

    // -- todo

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
        } else if (klass == RemoteDataflowBroadcast.class) {
            DataflowVariable remoteVar = remoteBroadcasts.get(name);
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
        if (klass == DataflowBroadcast.class) {
            return klass.cast(publishedBroadcasts.get(name));
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

    private <T> DataflowVariable<T> getPromise(Map<String, DataflowVariable<T>> registry, String name, String host, int port, SerialMsg requestMsg) {
        DataflowVariable remoteVariable = registry.get(name);
        if (remoteVariable == null) {
            DataflowVariable newRemoteVariable = new DataflowVariable<>();
            remoteVariable = registry.putIfAbsent(name, newRemoteVariable);
            if (remoteVariable == null) {
                createRequest(host, port, requestMsg);
                remoteVariable = newRemoteVariable;
            }

        }
        return remoteVariable;
    }

    /*
    public static void getDataflowReadChannel(String host, int port, String name) {
        if (localHost == null) {
            localHost = new LocalHost();
        }

        NettyClient client = new NettyClient(localHost, host, port, connection -> {
            if (connection.getHost() != null)
                connection.write();
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
