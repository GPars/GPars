package c19;
 
import org.jcsp.lang.*
import org.jcsp.net.*

class AccessSender implements CSProcess {

  def ChannelOutput toAccessServer         // connection to Sender

  void run() {
    
    //AClient = new LASAMAccessClient()
    //AClient = new AccessClient()
    def AClient = new AccessClientProcess()
    
    while (true) {
      toAccessServer.write(AClient)
      println "AccessSender: AccessClient sent to AccessServer"
    }
  }
}
