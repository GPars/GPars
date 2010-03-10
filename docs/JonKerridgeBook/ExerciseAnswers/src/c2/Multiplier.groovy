package c2

import org.jcsp.lang.*
 
class Multiplier implements CSProcess {
  
  def ChannelOutput outChannel
  def ChannelInput inChannel
  def int factor = 2
  
  void run() {
    def i = inChannel.read()
    while (i > 0) {
      outChannel.write( i * factor)
      i = inChannel.read()
    }
    outChannel.write(i)
  }
}

    
