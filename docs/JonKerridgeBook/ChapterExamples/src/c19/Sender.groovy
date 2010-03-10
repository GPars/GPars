package c19;
  
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.groovy.*

class Sender implements CSProcess {
  // one2one channel connections to Server
  def ChannelOutput toServer
  def ChannelInput fromServer
  // one2one channel used to indicate a client can be reused
  def ChannelInput reuse
  // list of  clients that are available to this sender
  def List clients

  void run() {
    // create the no service curently available mobile client
    //serviceUnavailable = new NoServiceClient()
    def serviceUnavailable = new NoServiceClientProcess()
    // determine the number of clients and create the list of available clients
    def n = clients.size()
    def clientsAvailable = []
    for (i in 0 ..< n) {
      clientsAvailable.add(clients[i])
      println " Sender: added client ${i} to available list"
    }
    
    //println "Sender: Clients added to available list"
    // create the alternative over fromServer and reuse
    def alt = new ALT ( [reuse, fromServer])
    while (true) {
      def index = alt.select()
      if (index == 0 ) {
        def use = reuse.read()
        clientsAvailable.add(clients[use])
        println "Sender: Client ${use} is being reused"
      }
      else {
        fromServer.read()  // its just a signal so do not need to save it
        if (clientsAvailable.size() > 0 ) {
          toServer.write(clientsAvailable.pop())
          println "Sender: Client sent to Server"
        }
        else {
          toServer.write(serviceUnavailable)
          println "Sender: Service Unavailable Client sent to Server"
        }
      }
      
    }
  }
}
