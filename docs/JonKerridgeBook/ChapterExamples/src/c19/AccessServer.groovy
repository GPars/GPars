package c19;
 
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.mobile.*
import org.jcsp.groovy.*
import phw.util.*

class AccessServer implements CSProcess {

  def ChannelInput fromAccessSender
  
  void run() {  
    def theServer = new MultiMobileProcessServer("A", fromAccessSender)
    //theServer.init("A", fromAccessSender)
    println " AccessServer: about to run"
    new PAR ([theServer]).run()
  }
}
