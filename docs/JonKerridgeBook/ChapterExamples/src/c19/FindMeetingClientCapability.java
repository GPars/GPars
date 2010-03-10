package c19;

import org.jcsp.net.*;
import org.jcsp.lang.*;
import org.jcsp.net.mobile.*;

/**
 * @author JM Kerridge
 *
 */
public class  FindMeetingClientCapability implements CSProcess {
  // location of net input channel to meeting organiser
  private NetChannelLocation clientServerLocation;
  private int clientId;
  
  // event channels that connect UI to FMCC
  private ChannelInput meetingNameEvent;
  
  // configure channels that connect FMCC to UI
  private ChannelOutput registeredConfigure;
  private ChannelOutput registeredLocationConfigure;
  private ChannelOutput attendeesConfigure;

  /**
 * @param clientServerLocation
 * @param clientId
 * @param meetingNameEvent
 * @param registeredConfigure
 * @param registeredLocationConfigure
 * @param attendeesConfigure
 */
public FindMeetingClientCapability(NetChannelLocation clientServerLocation, 
		  int clientId, 
		  ChannelInput meetingNameEvent, 
		  ChannelOutput registeredConfigure, 
		  ChannelOutput registeredLocationConfigure, 
		  ChannelOutput attendeesConfigure) 
  {
	this.clientServerLocation = clientServerLocation;
	this.clientId = clientId;
	this.meetingNameEvent = meetingNameEvent;
	this.registeredConfigure = registeredConfigure;
	this.registeredLocationConfigure = registeredLocationConfigure;
	this.attendeesConfigure = attendeesConfigure;
  }

/* (non-Javadoc)
 * @see com.quickstone.jcsp.lang.CSProcess#run()
 */
public void run () 
  {
    final NetChannelOutput client2Server = Mobile.createOne2Net(clientServerLocation);
    final NetChannelInput  server2Client = Mobile.createNet2One();
    final MeetingData clientData = new MeetingData();
    clientData.setReturnChannel ( server2Client.getChannelLocation() );
    clientData.setClientId ( clientId );
    // read data obtained from User interface into clientData using EventInput(s)
    clientData.setMeetingName ( (String) meetingNameEvent.read() );
    client2Server.write(clientData);
    final MeetingData  replyData = (MeetingData) server2Client.read();
    // write data from replyData into User Interface using ConfigureOutput(s)
    if ( replyData.getAttendees() == 0 ) 
    {
      registeredConfigure.write("NOT Registered");
    }
    else 
    {
      registeredConfigure.write("Registered");
      registeredLocationConfigure.write(replyData.getMeetingPlace() );
      attendeesConfigure.write(new String (" " + replyData.getAttendees()) );
    }
  }
    
}