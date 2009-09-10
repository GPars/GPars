package org.gparallelizer.remote

import org.gparallelizer.remote.netty.NettyTransportProvider

public class NettyTest extends CommunicationTestBase {
  static int port = 5239

  RemoteTransportProvider getTransportProvider () {
    new NettyTransportProvider()
  }
}