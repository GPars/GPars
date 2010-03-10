package c10

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Receiver implements CSProcess {
	
  def ChannelInput fromElement
  def ChannelOutput outChannel
  def int element
  
  def void run() {
    while (true) {
      def packet = fromElement.read()
      //println "Receiver ${element} has read " + packet.toString()
      outChannel.write ("Received: " + packet.toString() + "\n")
    }
  }
}

