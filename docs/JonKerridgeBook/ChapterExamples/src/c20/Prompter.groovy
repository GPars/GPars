package c20
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class Prompter implements CSProcess{
  
  def ChannelOutput toQueue
  def ChannelInput fromQueue
  def ChannelOutput toReceiver

  void run() {
    while (true) {
      toQueue.write(1)
      toReceiver.write ( fromQueue.read() )
    }    
  }
}