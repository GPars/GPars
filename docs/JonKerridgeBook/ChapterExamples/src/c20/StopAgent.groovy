package c20;
 
import org.jcsp.net.mobile.*
import org.jcsp.net.*
import org.jcsp.lang.*
import org.jcsp.groovy.*

class StopAgent implements MobileAgent {
  
  def ChannelOutput toLocal
  def ChannelInput fromLocal
  def int homeNode
  def int previousNode
  def boolean initialised
  def NetChannelLocation nextNodeInputEnd
                  
  def connect (List c) {
    this.toLocal = c[0]
    this.fromLocal = c[1]

  }
  
  def disconnect () {
    this.toLocal = null
    this.fromLocal = null
  }

  void run() {
    println "SA: running $homeNode, $previousNode, $initialised"
    toLocal.write(homeNode)	// tells node not to send to this node
    toLocal.write(previousNode) // where we want to get to
    toLocal.write(initialised)
    if ( ! initialised) {
      nextNodeInputEnd = fromLocal.read()
      initialised = true
      println "SA: initialised"
    }
    def gotThere = fromLocal.read()
    if ( gotThere ) {
      toLocal.write(nextNodeInputEnd)
      println "SA: got to $previousNode"
    }
  }

}