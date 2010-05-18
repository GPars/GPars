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
import org.jcsp.lang.ProcessManager;
import org.jcsp.net.NetChannelEnd;
import org.jcsp.net.NetChannelInput;
import org.jcsp.net.NetChannelLocation;
import org.jcsp.net.NetChannelOutput;
import org.jcsp.net.cns.CNS;
import org.jcsp.net.mobile.*;

public class NoServiceClientCapability implements CSProcess {
    private ChannelInput responseEvent;
    private ChannelOutput messageConfigure;

    public NoServiceClientCapability(ChannelInput responseEvent, ChannelOutput messageConfigure) {
        this.responseEvent = responseEvent;
        this.messageConfigure = messageConfigure;
    }

    public void run() {
        System.out.println("No service client capability started ");
        messageConfigure.write("Service Currently Unavailable");
        String response = (String) responseEvent.read();
        while (response != "Retry" && response != "Close") {
            response = (String) responseEvent.read();
        }
        System.out.println("NSCC: response is " + response);
        if (response == "Retry") {
            // then person wants to retry
            System.out.println("NSCC: retry requested");
            try {
                String processService = "A";
                final NetChannelLocation serverLoc = CNS.resolve(processService);
                final NetChannelOutput toServer = NetChannelEnd.createOne2Net(serverLoc);
                final NetChannelInput processReceive = Mobile.createNet2One();
                toServer.write(processReceive.getChannelLocation());
                final MobileProcess theProcess = (MobileProcess) processReceive.read();
                System.out.println("The access client has been received for service ");
                new ProcessManager(theProcess).run();
                System.out.println("The access client has finished for service ");
            }
            catch (Exception e) {
                System.out.println("Failed to connect to Server");
                System.exit(-1);
            }
        } else {
            System.out.println("NSCC: close requested");
            System.exit(-1);
        }
    }
}


