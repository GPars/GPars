package c20
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class Sender implements CSProcess {
	
  def ChannelOutput toElement
  def int element
  def int nodes
  def int iterations
  
  def void run() {
    def timer = new CSTimer()
    def start = element
    for ( i in 1 .. iterations ) {
      def dest = (start % (nodes) ) + 1
      if ( dest != element ) {
        def packet = new RingPacket ( source: element, destination: dest , value: (element * 10000) + start , full: true)
        toElement.write(packet)
        timer.sleep(500)
      }
      start = start + 1
    }
    println "Sender $element has finished"
  }
}

    
