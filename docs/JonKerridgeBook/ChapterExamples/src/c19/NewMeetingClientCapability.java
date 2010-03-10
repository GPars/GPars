// GPars (formerly GParallelizer)
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package c19;

import org.jcsp.lang.CSProcess;
import org.jcsp.lang.ChannelInput;
import org.jcsp.lang.ChannelOutput;
import org.jcsp.net.NetChannelInput;
import org.jcsp.net.NetChannelLocation;
import org.jcsp.net.NetChannelOutput;
import org.jcsp.net.mobile.*;

public class NewMeetingClientCapability implements CSProcess {
    // location of net input channel to meeting organiser
    private int clientId;
    private NetChannelLocation clientServerLocation;

    // event channels that connect UI to NMCC
    private ChannelInput meetingNameEvent;
    private ChannelInput meetingLocationEvent;

    // configure channels that connect NMCC to UI
    private ChannelOutput registeredConfigure;
    private ChannelOutput registeredLocationConfigure;
    /**
     * Channel used to write the number of people attending the meeting
     */
    private ChannelOutput attendeesConfigure;


    /**
     * @param clientId
     * @param clientServerLocation
     * @param meetingNameEvent
     * @param meetingLocationEvent
     * @param registeredConfigure
     * @param registeredLocationConfigure
     * @param attendeesConfigure
     */
    public NewMeetingClientCapability(int clientId, NetChannelLocation clientServerLocation, ChannelInput meetingNameEvent, ChannelInput meetingLocationEvent, ChannelOutput registeredConfigure, ChannelOutput registeredLocationConfigure, ChannelOutput attendeesConfigure) {
        this.clientId = clientId;
        this.clientServerLocation = clientServerLocation;
        this.meetingNameEvent = meetingNameEvent;
        this.meetingLocationEvent = meetingLocationEvent;
        this.registeredConfigure = registeredConfigure;
        this.registeredLocationConfigure = registeredLocationConfigure;
        this.attendeesConfigure = attendeesConfigure;
    }


    public void run() {
        System.out.println("New Meeting Client Capability");
        final NetChannelOutput client2Server = Mobile.createOne2Net(clientServerLocation);
        final NetChannelInput server2Client = Mobile.createNet2One();
        MeetingData clientData = new MeetingData();
        clientData.setReturnChannel(server2Client.getChannelLocation());
        clientData.setClientId(clientId);
        // read data obtained from User interface into clientData using EventInput(s)
        clientData.setMeetingName((String) meetingNameEvent.read());
        clientData.setMeetingPlace((String) meetingLocationEvent.read());
        client2Server.write(clientData);
        final MeetingData replyData = (MeetingData) server2Client.read();
        // write data from replyData into User Interface using ConfigureOutput(s)
        if (replyData.getAttendees() == 1) {
            registeredConfigure.write("Registered");
        } else {
            registeredConfigure.write("ALREADY Registered");
        }
        registeredLocationConfigure.write(replyData.getMeetingPlace());
//    System.out.println ( "New Meeting Client Capability about to write attendees ${replyData.attendees}"
        attendeesConfigure.write(new String(" " + replyData.getAttendees()));
        System.out.println("New Meeting Client Capability closing");
    }
}