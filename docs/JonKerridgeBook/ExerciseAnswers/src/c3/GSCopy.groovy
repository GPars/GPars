package c3 

import org.jcsp.lang.*
import org.jcsp.groovy.*

class GSCopy implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel0
  def ChannelOutput outChannel1
  
  void run () {
     while (true) {
      def i = inChannel.read()
      outChannel0.write(i)
      outChannel1.write(i)
    }
  }
}
