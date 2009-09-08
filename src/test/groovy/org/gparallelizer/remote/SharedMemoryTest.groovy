package org.gparallelizer.remote

import org.gparallelizer.remote.memory.SharedMemoryTransportProvider;

public class SharedMemoryTest extends CommunicationTestBase {
  RemoteTransportProvider getTransportProvider () {
    SharedMemoryTransportProvider.instance
  }
}