package c19;

import org.jcsp.net.*;
import org.jcsp.lang.*;
import org.jcsp.net.mobile.*;
import org.jcsp.util.*;

public class  NewMeetingClientProcess extends MobileProcess {
  
  static final long serialVersionUID = 9;
  
  // location of net input channel to meeting organiser
  private NetChannelLocation clientServerLocation;
  private int clientId;
  
  public NewMeetingClientProcess(NetChannelLocation clientServerLocation, int clientId) {
	this.clientServerLocation = clientServerLocation;
	this.clientId = clientId;
}

public void run () {
    // create internal event and configure channels required by the GUI
    final One2OneChannel meetingNameEvent = Channel.createOne2One(new OverWriteOldestBuffer (10) );
    final One2OneChannel meetingLocationEvent = Channel.createOne2One(new OverWriteOldestBuffer (10) );

    final One2OneChannel registeredConfigure = Channel.createOne2One(new OverWriteOldestBuffer (10) );
    final One2OneChannel registeredLocationConfigure = Channel.createOne2One(new OverWriteOldestBuffer (10) );
    final One2OneChannel attendeesConfigure = Channel.createOne2One(new OverWriteOldestBuffer (10) );
    //
    System.out.println ( "New Meeting Client Process started" );
    final CSProcess [] network = { 
    		new NewMeetingClientCapability ( 
                                clientId,
                                clientServerLocation,
                                meetingNameEvent.in(),
                                meetingLocationEvent.in(),
                                registeredConfigure.out(),
                                registeredLocationConfigure.out(),
                                attendeesConfigure.out() ),
            new NewMeetingClientUserInterface ( 
                                meetingNameEvent.out(),
                                meetingLocationEvent.out(),
                                registeredConfigure.in(),
                                registeredLocationConfigure.in(),
                                attendeesConfigure.in() ) };
    new Parallel ( network ).run();
  }
}

