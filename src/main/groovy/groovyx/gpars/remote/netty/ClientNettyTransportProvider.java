package groovyx.gpars.remote.netty;

import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.LocalNode;

public class ClientNettyTransportProvider extends LocalHost {
    final NettyClient client;

    public ClientNettyTransportProvider(String address, int port) throws InterruptedException {
        client = new NettyClient(this, address, port);
    }

    @Override
    public void connect(LocalNode node) throws InterruptedException {
        super.connect(node);
        client.start();
        System.err.printf("Client connected to: %s%n", client);
    }

    @Override
    public void disconnect() throws InterruptedException {
        super.disconnect();
        client.stop();
        System.err.println("Client stopped");
    }
}
