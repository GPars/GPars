package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelOutput
import org.jcsp.lang.ChannelInput

class GTail implements CSProcess {
  
  def ChannelOutput outChannel
  def ChannelInput inChannel
  
  void run () {
    inChannel.read()
    while (true) {
      outChannel.write( inChannel.read() )
    }
  }
}
