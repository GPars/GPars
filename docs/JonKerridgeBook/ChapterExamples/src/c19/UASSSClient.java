package c19;

import org.jcsp.lang.*;
import org.jcsp.net.*;
import org.jcsp.net.cns.*;
import org.jcsp.net.tcpip.*;
import org.jcsp.net.mobile.*;

import phw.util.Ask;

/**
 *
 * <p>Client process allowing the reception of <code>{@link MobileProcess}</code> objects to run.</p>
 * <H2>Description</H2>
 * <p>This class allows the reception of <code>{@link MobileProcess}</code> objects from a
 * <code>{@link MobileProcessServer}</code>, and then run them.  There are two possible implementations
 * available.  The client can either be run as a standalone program, and hence receive just one process
 * which it will run, or it may be run in a <code>Parallel</code> construct.  If the latter option is
 * taken, the client process must be passed service names into its <code>serviceNameInput</code> channel
 * as a <code>String</code>.  This name is then used to resolve a channel with the CNS.  The process
 * then retrieves a <code>{@link MobileProcess}</code> from the <code>{@link MobileProcessServer}</code>
 * using the service name.  When the <code>{@link MobileProcess}</code> is received, it is written to the
 * <code>processOut</code> channel, and used as necessary.</p>
 * <p>The client process is a Singleton, and as such only one is ever in existence.  To retrieve this
 * instance, the <code>getClient</code> method needs to be used.  Before being used, the client needs to
 * be initialised using the <code>init</code> method, passing the channel to be used for service names
 * and the one to be used to pass processes out.</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Napier University</p>
 * @author Kevin Chalmers
 * @version 1.0
 *
 * @author Jon Kerridge
 * @version 1.1 ( simple client for a PDA running a single process )
 * <p> PDA Version comprises just a main program and assumes that the PDA is going to run a single
 * client process, which may contains nested parallels.  The PDA does NOT directly communicate with 
 * any other processors.  This version allows the PDA to run one client processes on a
 * default network. Still need to allow multiple clients on different networks.</p>
 */

public class UASSSClient {

  /**
   * Channel used to receive <code>{@link MobileProcess}</code> objects from a
   * <code>{@link MobileProcessServer}</code>
   */
  private static NetAltingChannelInput processReceive;


  /**
   * Main method used to start standalone client
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
    }
    else {
      Mobile.init(Node.getInstance().init(new TCPIPNodeFactory(CNS_IP)));
    }
    //Mobile.init(Node.getInstance().init(new TCPIPNodeFactory(CNS_IP)));
    String processService = "A";

    // Get location of server
    NetChannelLocation serverLoc = CNS.resolve(processService);

    // Create output channel to server
    NetChannelOutput toServer = NetChannelEnd.createOne2Net(serverLoc);

    // Create input channel from server for process
    processReceive =  Mobile.createNet2One();

    // Write location of input channel to server
    toServer.write(processReceive.getChannelLocation());

    // Receive process and run it
    MobileProcess theProcess = (MobileProcess)processReceive.read();
    System.out.println("The access client has been received for service: " + CNS_IP);
    new ProcessManager(theProcess).run();
    System.out.println("The access client has finished for service: " + CNS_IP);
    }
    catch (NodeInitFailedException e)  {
      System.out.println("Failed to connect to Server");
      System.exit(-1);
    }
  }
}