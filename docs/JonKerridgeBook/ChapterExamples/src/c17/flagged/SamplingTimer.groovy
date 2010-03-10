package c17.flagged

import org.jcsp.groovy.*
import org.jcsp.lang.*

class SamplingTimer implements CSProcess {

  def ChannelOutput sampleRequest
  def sampleInterval
  
  void run() {
    def timer = new CSTimer()
    while (true){
      timer.sleep(sampleInterval)
      sampleRequest.write(1)
    }
  }
}