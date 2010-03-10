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

import org.jcsp.lang.ProcessManager;
import org.jcsp.net.NetAltingChannelInput;
import org.jcsp.net.NetChannelEnd;
import org.jcsp.net.NetChannelLocation;
import org.jcsp.net.NetChannelOutput;
import org.jcsp.net.Node;
import org.jcsp.net.NodeInitFailedException;
import org.jcsp.net.cns.CNS;
import org.jcsp.net.mobile.*;
import org.jcsp.net.tcpip.TCPIPNodeFactory;
import phw.util.Ask;

public class UASSSClient {

    /**
     * Channel used to receive <code>{@link MobileProcess}</code> objects from a
     * <code>{@link MobileProcessServer}</code>
     */
    private static NetAltingChannelInput processReceive;


    /**
     * Main method used to start standalone client
     *
     * @param args - Command line input
     */
    public static void main(String[] args) {
        // Get CNS_IP and service name
        String CNS_IP = Ask.string("Enter IP address of CNS: ");
        //String CNS_IP = "146.176.162.75";     // IP Address of CNS on Network
        //String CNS_IP = "169.254.171.140";  // IP address of Jon's laptop
        //String CNS_IP = null;

        try {
            // Initialise Mobile package
            if (CNS_IP == null) {
                Mobile.init(Node.getInstance().init(new TCPIPNodeFactory()));
            } else {
                Mobile.init(Node.getInstance().init(new TCPIPNodeFactory(CNS_IP)));
            }
            //Mobile.init(Node.getInstance().init(new TCPIPNodeFactory(CNS_IP)));
            String processService = "A";

            // Get location of server
            NetChannelLocation serverLoc = CNS.resolve(processService);

            // Create output channel to server
            NetChannelOutput toServer = NetChannelEnd.createOne2Net(serverLoc);

            // Create input channel from server for process
            processReceive = Mobile.createNet2One();

            // Write location of input channel to server
            toServer.write(processReceive.getChannelLocation());

            // Receive process and run it
            MobileProcess theProcess = (MobileProcess) processReceive.read();
            System.out.println("The access client has been received for service: " + CNS_IP);
            new ProcessManager(theProcess).run();
            System.out.println("The access client has finished for service: " + CNS_IP);
        }
        catch (NodeInitFailedException e) {
            System.out.println("Failed to connect to Server");
            System.exit(-1);
        }
    }
}