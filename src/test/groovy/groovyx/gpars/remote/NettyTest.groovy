package groovyx.gpars.remote

import groovyx.gpars.remote.netty.NettyTransportProvider

public class NettyTest extends CommunicationTestBase {
    static int port = 5239

    LocalHost getTransportProvider() {
        new NettyTransportProvider()
    }
}