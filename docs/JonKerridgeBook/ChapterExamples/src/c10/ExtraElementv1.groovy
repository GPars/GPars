package c10;


import org.jcsp.lang.*
import org.jcsp.groovy.*

class ExtraElementv1 implements CSProcess {
  def ChannelInput fromRing
  def ChannelOutput toRing
  
  def void run () {
    while (true) {
      def packet = (RingPacket) fromRing.read()
      println "Extra Element 0 has read " + packet.toString()
      toRing.write( packet )
      println "Extra Element 0 has written " + packet.toString()
    }
  }
}