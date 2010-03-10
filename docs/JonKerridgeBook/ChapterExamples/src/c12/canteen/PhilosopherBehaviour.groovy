package c12.canteen

import org.jcsp.lang.*
import org.jcsp.groovy.*

class PhilosopherBehaviour implements CSProcess {
  
  def int id  = -1
  def ChannelOutput service
  def ChannelInput deliver
  def ChannelOutput toConsole
    
  def void run() {
    def tim = new CSTimer()
    toConsole.write( "Starting ... \n")
    while (true) {
      toConsole.write( "Thinking ... \n")
      if (id > 0) {
        tim.sleep (3000)  
      }
      else {
        // Philosopher 0, has a 0.1 second think
        tim.sleep (100)
      }
      toConsole.write( "Need a chicken ...\n")
      service.write(id)
      def gotOne = deliver.read()
      if ( gotOne > 0 ) {
        toConsole.write( "Eating ... \n")
        tim.sleep (2000)  
        toConsole.write( "Brrrp ... \n")
      }
      else {
        toConsole.write( "                   Oh dear No chickens left \n")
      }  
    }
  }
  
}
