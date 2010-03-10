package c12.fork

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Butler implements CSProcess {
  
  def ChannelInputList enters
  def ChannelInputList exits
  
  def void run() {
    def seats = enters.size()
    def seated = 0
    
    def allChans = []
    for ( i in 0 ..< seats ) { allChans << exits[i]  }
    for ( i in 0 ..< seats ) { allChans << enters[i] }
    
    def eitherAlt = new ALT ( allChans )
    def exitAlt = new ALT ( exits )
    
    while (true) {
      def spaces = seated < ( seats - 1 )
      def usedAlt = spaces ? eitherAlt : exitAlt
      def i = usedAlt.select()
      allChans[i].read()
      def exiting = i < seats
      seated = exiting ? seated - 1 : seated + 1
    } // end while
  } //end run
} // end class

