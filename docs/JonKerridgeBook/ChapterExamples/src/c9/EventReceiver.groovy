package c9;

import org.jcsp.lang.*
import org.jcsp.groovy.*

class EventReceiver implements CSProcess {
  
  def ChannelInput eventIn
  def ChannelOutput eventOut

  void run() {
    while (true){
      eventOut.write(eventIn.read())
    }    
  }
}