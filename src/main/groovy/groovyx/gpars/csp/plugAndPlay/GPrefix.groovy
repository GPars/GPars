package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.ChannelOutput

class GPrefix implements CSProcess {
  
  def int prefixValue = 0
  def ChannelInput inChannel
  def ChannelOutput outChannel
  
  void run () {
    outChannel.write(prefixValue)
    while (true) {
      outChannel.write( inChannel.read() )
    }
  }
}
