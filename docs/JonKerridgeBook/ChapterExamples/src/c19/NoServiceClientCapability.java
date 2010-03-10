package c19;

import org.jcsp.net.*;
import org.jcsp.net.cns.*;
import org.jcsp.lang.*;
import org.jcsp.net.mobile.*;

public class  NoServiceClientCapability implements CSProcess {
  private ChannelInput responseEvent ;
  private ChannelOutput messageConfigure ;
  
  public NoServiceClientCapability(ChannelInput responseEvent, ChannelOutput messageConfigure) {
	this.responseEvent = responseEvent;
	this.messageConfigure = messageConfigure;
}

public void run() {
    System.out.println ( "No service client capability started " );
    messageConfigure.write ( "Service Currently Unavailable" );
    String response = (String) responseEvent.read();
    while ( ( response != "Retry" ) && ( response != "Close" ) ) {
      response = (String) responseEvent.read();
    }
    System.out.println ( "NSCC: response is " + response );
    if ( response == "Retry" ) {
      // then person wants to retry
      System.out.println ( "NSCC: retry requested" );
      try {
        String processService = "A";
        final NetChannelLocation serverLoc = CNS.resolve(processService);
        final NetChannelOutput toServer = NetChannelEnd.createOne2Net(serverLoc);
        final NetChannelInput processReceive = Mobile.createNet2One();
        toServer.write(processReceive.getChannelLocation());
        final MobileProcess theProcess = (MobileProcess)processReceive.read();
        System.out.println ( "The access client has been received for service " );
        new ProcessManager(theProcess).run();
        System.out.println ( "The access client has finished for service " );
      }
      catch (Exception e)  {
        System.out.println ( "Failed to connect to Server" );
        System.exit(-1);
      }
    }
    else {
      System.out.println ( "NSCC: close requested" );
      System.exit(-1);
    }
  }
}


