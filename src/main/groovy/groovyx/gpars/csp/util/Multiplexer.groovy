package groovyx.gpars.csp.util

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelOutput
import groovyx.gpars.csp.ChannelInputList
import groovyx.gpars.csp.ALT

class Multiplexer implements CSProcess {
 
  def ChannelInputList inChannels
  def ChannelOutput outChannel

  void run () {  
    def alt = new ALT( inChannels )
    while (true) {
      def index = alt.select ()
      outChannel.write (inChannels[index].read())
    } 
  } 
}
