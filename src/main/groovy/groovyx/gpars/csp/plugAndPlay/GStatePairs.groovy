package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelOutput
import org.jcsp.lang.ChannelInput

class GStatePairs implements CSProcess {
  
  def ChannelOutput outChannel
  def ChannelInput  inChannel
  
  void run() {
    
    def n1 = inChannel.read()
    def n2 = inChannel.read()
    while (true) {
      outChannel.write ( n1 + n2 )
      n1 = n2
      n2 = inChannel.read()
    }
  }
}
