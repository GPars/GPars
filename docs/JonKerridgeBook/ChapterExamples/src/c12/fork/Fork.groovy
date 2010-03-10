package c12.fork

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Fork implements CSProcess {
  
  def ChannelInput left
  def ChannelInput right
  
  def void run () {
    def fromPhilosopher = [left, right]
    def forkAlt = new ALT ( fromPhilosopher )
    while (true) {
      def i = forkAlt.select()
      fromPhilosopher[i].read()      //pick up fork i
      fromPhilosopher[i].read()      //put down fork i
    }
  }
}

      
    