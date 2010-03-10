package c17.counted

import org.jcsp.groovy.*
import org.jcsp.lang.*

class CountingSampler implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel
  def ChannelInput sampleRequest
  def ChannelOutput countReturn

  void run() {
    def sampleAlt = new ALT ([sampleRequest, inChannel])
    def counter = 0
    while (true){
      counter = counter + 1
      def index = sampleAlt.priSelect()
      if (index == 0) {
        sampleRequest.read()
        def v = inChannel.read()
        outChannel.write(v)
        println "Sampled Value was ${v}"
        countReturn.write(counter)
      }
      else {
        outChannel.write(inChannel.read())
      }
    }
  }
}