package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import groovyx.gpars.csp.ChannelInputList
import org.jcsp.plugNplay.ProcessRead
import groovyx.gpars.csp.PAR
import org.jcsp.lang.CSTimer

class GParPrint implements CSProcess {
  
  def ChannelInputList inChannels
  def List headings
  def long delay = 200
  
  void run() {
    def inSize = inChannels.size()
    def readerList = []
    (0 ..< inSize).each { i -> 
      readerList [i] = new ProcessRead ( inChannels[i] ) 
    }
    
    def parRead = new PAR ( readerList )

    if ( headings == null ) {
      println "No headings provided"
    }
    else {
      headings.each { print "\t${it}" }
      println ()
    }

    def timer = new CSTimer()
    while ( true) {
      parRead.run()
 //     print "\t"
//      readerList.each { pr -> print "${pr.value}\t" }
      readerList.each { pr -> print "\t" + pr.value.toString() }
      println ()
      if (delay > 0 ) {
        timer.sleep ( delay)
      }
    }
  }
}