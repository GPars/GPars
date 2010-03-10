package c18

import org.jcsp.net.*
import org.jcsp.lang.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import org.jcsp.groovy.*
 
class ForwardBackAgent implements MobileAgent {
  
  def ChannelOutput toLocal
  def ChannelInput fromLocal
  def NetChannelLocation backChannel
  def results = [ ]
                  
  def connect ( List c ) {
    this.toLocal = c[0]
    this.fromLocal = c[1]
  }
  
  def disconnect (){
    toLocal = null
    fromLocal = null
  }

  void run() {
    def fromRoot = NetChannelEnd.createNet2One()
    def fromRootLocation = fromRoot.getChannelLocation()
    def toRoot = NetChannelEnd.createOne2Net (backChannel)
    toRoot.write(fromRootLocation)
    results << fromRoot.read()
    toLocal.write (results)
    results = fromLocal.read()
    def last = results.size - 1
    toRoot.write(results[last])
    toRoot.destroyWriter()
    fromRoot.destroyReader()
  }

}