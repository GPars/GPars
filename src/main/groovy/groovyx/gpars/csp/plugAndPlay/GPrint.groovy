package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.CSTimer

class GPrint implements CSProcess {
  
  def ChannelInput inChannel
  def String heading = "No Heading Provided"
  def long delay = 200
  
  def void run() {
    def timer = new CSTimer()
    println "${heading}"
    while ( true) {
      println inChannel.read().toString()
      if (delay != 0 ) {
        timer.sleep ( delay)
      }
    }
  }
}