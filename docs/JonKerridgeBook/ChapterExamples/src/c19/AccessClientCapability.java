package c19;

import org.jcsp.net.*;
import org.jcsp.net.cns.*;
import org.jcsp.lang.*;
import org.jcsp.net.mobile.*;


/**
 * @author JM Kerridge
 *
 */
public class AccessClientCapability implements CSProcess {
  private ChannelInput eventChannel;
  
  /**
 * @param eventChannel
 */
public AccessClientCapability(ChannelInput eventChannel) {
	this.eventChannel = eventChannel;
}

public void run () {
    // read in the type of service required
    System.out.println ("Service Chooser has started");
    final String eventType = (String) eventChannel.read();
    //System.out.println ( "Event received: " + eventType );
    String serviceName = ( eventType == "Create New Meeting" ) ? "N" : "F" ;
    /*
    String serviceName = null;
    if ( eventType == "Create New Meeting" ) {
      serviceName = "N";
    }
    else {
      serviceName = "F";
    }
    */
    System.out.println ( "Selected service: " + serviceName );
    // now connect to service required
    // Get location of server
    final NetChannelLocation serverLoc = CNS.resolve(serviceName);
    // Create output channel to server
    final NetChannelOutput toServer = NetChannelEnd.createOne2Net(serverLoc);
    // Create input channel from server for process
    final NetChannelInput processReceive = Mobile.createNet2One();
    // Write location of input channel to server
    toServer.write(processReceive.getChannelLocation());
    // Receive process and run it;
    final MobileProcess theProcess = (MobileProcess)processReceive.read();
    System.out.println ("The client has been received for service: " + eventType );
    new ProcessManager(theProcess).run();
  }
}
