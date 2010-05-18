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
import org.jcsp.lang.ProcessManager;
import org.jcsp.net.NetChannelEnd;
import org.jcsp.net.NetChannelInput;
import org.jcsp.net.NetChannelLocation;
import org.jcsp.net.NetChannelOutput;
import org.jcsp.net.cns.CNS;
import org.jcsp.net.mobile.*;


/**
 * @author JM Kerridge
 */
public class AccessClientCapability implements CSProcess {
    private ChannelInput eventChannel;

    /**
     * @param eventChannel
     */
    public AccessClientCapability(ChannelInput eventChannel) {
        this.eventChannel = eventChannel;
    }

    public void run() {
        // read in the type of service required
        System.out.println("Service Chooser has started");
        final String eventType = (String) eventChannel.read();
        //System.out.println ( "Event received: " + eventType );
        String serviceName = eventType == "Create New Meeting" ? "N" : "F";
        /*
        String serviceName = null;
        if ( eventType == "Create New Meeting" ) {
          serviceName = "N";
        }
        else {
          serviceName = "F";
        }
        */
        System.out.println("Selected service: " + serviceName);
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
        final MobileProcess theProcess = (MobileProcess) processReceive.read();
        System.out.println("The client has been received for service: " + eventType);
        new ProcessManager(theProcess).run();
    }
}
