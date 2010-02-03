package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.ChannelOutput
import org.jcsp.lang.CSTimer

class GFixedDelay implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel
  def delay = 0
  
  void run () {
    def timer = new CSTimer()
    while (true) {
      def v = inChannel.read()
      timer.sleep( delay )
      outChannel.write( v )
    }
  }
}