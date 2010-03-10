package c2

import org.jcsp.lang.*

class ProduceHW implements CSProcess {
  
  def ChannelOutput outChannel
  
  void run() {
    def hi = "Hello"
    def thing = "World"
    outChannel.write ( hi )
    outChannel.write ( thing )   
  } 
}

      
  
  
