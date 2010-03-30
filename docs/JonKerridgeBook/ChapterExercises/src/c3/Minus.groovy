package c3
 
import org.jcsp.lang.*
import org.jcsp.plugNplay.ProcessRead
import org.jcsp.groovy.*

class Minus implements CSProcess {
  
  def ChannelInput inChannel0
  def ChannelInput inChannel1
  def ChannelOutput outChannel
  
  void run () {

    ProcessRead read0 = new ProcessRead ( inChannel0)
    ProcessRead read1 = new ProcessRead ( inChannel1)
    def parRead2 = new PAR ( [ read0, read1 ] )

    while (true) {
      parRead2.run()
      // output one value subtracted from the other
      // be certain you know which way round you are doing the subtraction!!
    }
  }
}
            