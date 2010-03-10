package c18

import org.jcsp.net.*
import org.jcsp.lang.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import org.jcsp.groovy.*
 
class TripAgent implements MobileAgent {
  
  def ChannelOutput toLocal
  def ChannelInput fromLocal
  def tripList = [ ]
  def results = [ ]
  def int pointer
                  
  def connect ( List c ) {
    this.toLocal = c[0]
    this.fromLocal = c[1]
  }
  
  def disconnect (){
    toLocal = null
    fromLocal = null
  }

  void run() {
    toLocal.write (results)
    results = fromLocal.read()
    if (pointer > 0) {
      pointer = pointer - 1
      def nextChannel = NetChannelEnd.createOne2Net (tripList.get(pointer))
      disconnect()
      nextChannel.write(this)
    }
    else {
      println "Agent has returned to TripRoot"
    }
  }

}