package c12.fork

import org.jcsp.lang.*
import org.jcsp.groovy.*

class LazyButler implements CSProcess {
  
  def ChannelInputList enters
  def ChannelInputList exits
  
  def void run() {
    def seats = enters.size()
    def allChans = []
    
    for ( i in 0 ..< seats ) { allChans << exits[i]  }
    for ( i in 0 ..< seats ) { allChans << enters[i] }
    
    def eitherAlt = new ALT ( allChans )
    
    while (true) {
      def i = eitherAlt.select()
      allChans[i].read()
    } // end while
  } //end run
} // end class