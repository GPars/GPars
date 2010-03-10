package c19;
 
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.mobile.*
import org.jcsp.groovy.*

class Server implements CSProcess {
  
  // connections to Sender
  def ChannelInput fromSender
  def ChannelOutput toSender
  // the name of the service
  def String serviceName
  
  void run() {  
    println "Server: initialising ${serviceName}"
    def theServer = new MultiMobileProcessServer(serviceName, fromSender, toSender)
    //theServer.init(serviceName, fromSender, toSender)
    println " Server: initialised for service ${serviceName}"
    new PAR ([theServer]).run()
  }
}
