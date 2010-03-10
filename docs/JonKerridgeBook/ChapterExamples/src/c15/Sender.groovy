package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Sender implements CSProcess {
  
  def ChannelOutput outChannel
  def String id
  
  void run() {
    def timer = new CSTimer()
    while (true) {
      timer.sleep(10000)
      outChannel.write ( id )
    }
  }
}

      
  
