package org.gparallelizer.remote

import org.gparallelizer.remote.sharedmemory.SharedMemoryTransportProvider
import org.gparallelizer.remote.nonsharedmemory.NonSharedMemoryTransportProvider;

public class NonSharedMemoryTest extends CommunicationTestBase {

  protected void setUp() {
    LocalNodeRegistry.removeTransportProvider SharedMemoryTransportProvider.instance
    LocalNodeRegistry.addTransportProvider NonSharedMemoryTransportProvider.instance
    super.setUp();
  }

  protected void tearDown() {
    LocalNodeRegistry.removeTransportProvider NonSharedMemoryTransportProvider.instance
    LocalNodeRegistry.addTransportProvider SharedMemoryTransportProvider.instance
    super.tearDown();
  }

}