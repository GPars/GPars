package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Receiver implements CSProcess {
  
  def ChannelInput inChannel
  
  void run() {
    while (true) {
      def v = inChannel.read()
      println "${v}"
    }
  }
}

