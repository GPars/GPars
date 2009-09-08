package org.gparallelizer.remote

import org.gparallelizer.remote.memory.NonSharedMemoryTransportProvider;

public class NonSharedMemoryTest extends CommunicationTestBase {
  RemoteTransportProvider getTransportProvider () {
    NonSharedMemoryTransportProvider.instance
  }

  protected void setUp() {
    super.setUp();
  }
}