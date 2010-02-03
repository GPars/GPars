package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.ChannelOutput

class GSuccessor implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel
  
  void run () {
    while (true) {
      outChannel.write( inChannel.read() + 1 )
    }
  }
}
