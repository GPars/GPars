package c21

 
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*

class Type1Process extends GroovyMobileProcess implements Serializable {
  def String toGathererName
  def ChannelInput inChannel
  def int nodeId
  
  def connect (l) {
    inChannel = l[0]
    nodeId = l[1]
    toGathererName = l[2]
  }
/*  
  def reconnect (l) {
    inChannel = l[0]
  }
*/
  def disconnect () {
    inChannel = null
  }

  void run() {
    def toGatherer = CNS.createAny2Net(toGathererName)
    while (true) {
      def Type1 d = inChannel.read()
      d.modify(nodeId)
      toGatherer.write(d)
    }    
  }

}