package groovyx.gpars.csp.util

import org.jcsp.lang.CSProcess
import groovyx.gpars.csp.ChannelInputList
import org.jcsp.lang.ChannelOutput
import groovyx.gpars.csp.ALT

class FairMultiplex implements CSProcess {
 
  def ChannelInputList inChannels
  def ChannelOutput outChannel

  def void run () {  
    def alt = new ALT( inChannels )
    while (true) {
      def index = alt.fairSelect ()
      outChannel.write (inChannels[index].read())
    } 
  } 
}
