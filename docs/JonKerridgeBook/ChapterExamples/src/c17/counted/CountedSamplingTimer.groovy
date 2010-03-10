package c17.counted

import org.jcsp.groovy.* 
import org.jcsp.lang.*

class CountedSamplingTimer implements CSProcess {

  def ChannelOutput sampleRequest
  def sampleInterval
  def ChannelInput countReturn
  def ChannelOutput countToGatherer
  
  void run() {
    def timer = new CSTimer()
    while (true){
      timer.sleep(sampleInterval)
      sampleRequest.write(1)
      def c= countReturn.read()
      countToGatherer.write(c)
    }
  }
}