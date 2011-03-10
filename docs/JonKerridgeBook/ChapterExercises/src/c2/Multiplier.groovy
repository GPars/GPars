package c2

import org.jcsp.lang.*
 
class Multiplier implements CSProcess {
  
  def ChannelOutput outChannel
  def ChannelInput inChannel
  def int factor = 2
  
  void run() {
    def i = inChannel.read()
    while (i > 0) {
      // write i * factor to outChannel
      // read in the next value of i
    }
    outChannel.write(i)
  }
}

    
