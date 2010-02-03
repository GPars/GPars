package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.ChannelOutput
import org.jcsp.plugNplay.ProcessWrite
import groovyx.gpars.csp.PAR

class GDelta2 implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel0
  def ChannelOutput outChannel1
  
  def void run () {
    def write0 = new ProcessWrite ( outChannel0)
    def write1 = new ProcessWrite ( outChannel1)
    def parWrite2 = new PAR ( [ write0, write1 ] )
    while (true) {
      def i = inChannel.read()
      write0.value = i
      write1.value = i
      parWrite2.run()
    }
  }
}
