package c10

import org.jcsp.lang.*
import org.jcsp.groovy.*

class ExtraElement implements CSProcess {
  def ChannelInput fromRing
  def ChannelOutput toRing
  
  def void run () {
    def packet = new RingPacket (source:-1, destination:-1, value:-1, full: false )
    while (true) {
      toRing.write( packet )
      println "Extra Element 0 has written " + packet.toString()
      packet = (RingPacket) fromRing.read()
      println "Extra Element 0 has read " + packet.toString()
    }
  }
}
 
      
