package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Put implements CSProcess {
  
  def ChannelOutput outChannel
  
  def void run() {
    def i = 1
    while (true) {
      outChannel.write ( i )
      i = i + 1
    }
  }
}

      
  
