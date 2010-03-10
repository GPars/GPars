package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Get implements CSProcess {
  
  def ChannelInput inChannel
  def int id
  
  def void run() {
    def timer = new CSTimer()
    while (true) {
      def v = inChannel.read()
      println "${id} .. ${v}"
      timer.sleep(200 * v)
    }
  }
}

