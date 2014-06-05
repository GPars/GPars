package groovyx.gpars.remote.netty;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.LocalNode;
import groovyx.gpars.serial.SerialContext;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class ClientNettyTransportProvider extends LocalHost {
    final NettyClient client;

    CountDownLatch connected;

    Actor remoteActor;

    public ClientNettyTransportProvider(String address, int port) throws InterruptedException {
        client = new NettyClient(this, address, port);
        connected = new CountDownLatch(1);
    }

    public Actor connect() throws InterruptedException {
        client.start();
        System.err.printf("Client connected to: %s%n", client);
        connected.await();
        return remoteActor;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        try {
            client.stop();
            System.err.println("Client stopped");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectRemoteNode(UUID nodeId, SerialContext host, Actor mainActor) {
        super.connectRemoteNode(nodeId, host, mainActor);
        remoteActor = mainActor;
        connected.countDown();
    }
}
