package c2

import phw.util.*
import org.jcsp.lang.*

class ProduceHN implements CSProcess {
  
  def ChannelOutput outChannel
  
  void run() {
    def hi = "Hello"
    def thing = Ask.string ("\nName ? ")
    outChannel.write ( hi )
    outChannel.write ( thing )   
  } 
}

      
  
  
