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

/**
 * @author JM Kerridge
 */
public class FindMeetingClientCapability implements CSProcess {
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
                                       ChannelOutput attendeesConfigure) {
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

    public void run() {
        final NetChannelOutput client2Server = Mobile.createOne2Net(clientServerLocation);
        final NetChannelInput server2Client = Mobile.createNet2One();
        final MeetingData clientData = new MeetingData();
        clientData.setReturnChannel(server2Client.getChannelLocation());
        clientData.setClientId(clientId);
        // read data obtained from User interface into clientData using EventInput(s)
        clientData.setMeetingName((String) meetingNameEvent.read());
        client2Server.write(clientData);
        final MeetingData replyData = (MeetingData) server2Client.read();
        // write data from replyData into User Interface using ConfigureOutput(s)
        if (replyData.getAttendees() == 0) {
            registeredConfigure.write("NOT Registered");
        } else {
            registeredConfigure.write("Registered");
            registeredLocationConfigure.write(replyData.getMeetingPlace());
            attendeesConfigure.write(new String(" " + replyData.getAttendees()));
        }
    }

}