package c3 
 
import org.jcsp.lang.*

class Negator implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel
  
  void run () {
    while (true) {
      outChannel.write( 0 -  inChannel.read() )
    }
  }
}
