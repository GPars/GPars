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
import org.jcsp.lang.Channel;
import org.jcsp.lang.One2OneChannel;
import org.jcsp.lang.Parallel;
import org.jcsp.net.NetChannelLocation;
import org.jcsp.net.mobile.*;
import org.jcsp.util.OverWriteOldestBuffer;

public class NewMeetingClientProcess extends MobileProcess {

    static final long serialVersionUID = 9;

    // location of net input channel to meeting organiser
    private NetChannelLocation clientServerLocation;
    private int clientId;

    public NewMeetingClientProcess(NetChannelLocation clientServerLocation, int clientId) {
        this.clientServerLocation = clientServerLocation;
        this.clientId = clientId;
    }

    public void run() {
        // create internal event and configure channels required by the GUI
        final One2OneChannel meetingNameEvent = Channel.createOne2One(new OverWriteOldestBuffer(10));
        final One2OneChannel meetingLocationEvent = Channel.createOne2One(new OverWriteOldestBuffer(10));

        final One2OneChannel registeredConfigure = Channel.createOne2One(new OverWriteOldestBuffer(10));
        final One2OneChannel registeredLocationConfigure = Channel.createOne2One(new OverWriteOldestBuffer(10));
        final One2OneChannel attendeesConfigure = Channel.createOne2One(new OverWriteOldestBuffer(10));
        //
        System.out.println("New Meeting Client Process started");
        final CSProcess[] network = {
                new NewMeetingClientCapability(
                        clientId,
                        clientServerLocation,
                        meetingNameEvent.in(),
                        meetingLocationEvent.in(),
                        registeredConfigure.out(),
                        registeredLocationConfigure.out(),
                        attendeesConfigure.out()),
                new NewMeetingClientUserInterface(
                        meetingNameEvent.out(),
                        meetingLocationEvent.out(),
                        registeredConfigure.in(),
                        registeredLocationConfigure.in(),
                        attendeesConfigure.in())};
        new Parallel(network).run();
    }
}

