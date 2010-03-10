package c17.flagged

import org.jcsp.groovy.*
import org.jcsp.lang.*

class SampledNetwork implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel

  void run() {
    while (true) {
      def v = inChannel.read()
      v.c = v.a + v.b
      outChannel.write(v)
    }
    
  }

}