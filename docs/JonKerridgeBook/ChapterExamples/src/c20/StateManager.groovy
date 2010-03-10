package c20;
 
import org.jcsp.lang.*

class StateManager implements CSProcess{
  
  def ChannelInput fromQueue
  def ChannelOutput toElement
  def int queueSlots

  void run() {
    def limit = queueSlots / 2
    def state = "NORMAL" 
    while (true) {
      def usedSlots = fromQueue.read()
      def aboveLimit = ( usedSlots >= limit)
      if ((state == "NORMAL") && ( aboveLimit)) {
        state = "ABOVE_LIMIT"
        toElement.write("STOP")
        println "SM: stopping"
      }
      if ((state == "ABOVE_LIMIT") && ( !aboveLimit)) {
        state = "NORMAL"
        toElement.write("RESTART")
        println "SM: restarting"
      }      
    }
  }

}