package c17.flagged

import org.jcsp.groovy.*
import org.jcsp.lang.*

class Sampler implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel
  def ChannelInput sampleRequest

  void run() {
    def sampleAlt = new ALT ([sampleRequest, inChannel])
    while (true){
      def index = sampleAlt.priSelect()
      if (index == 0) {
        sampleRequest.read()
        def v = inChannel.read()
        def fv = new FlaggedSystemData ( a: v.a, b:v.b, testFlag: true)
        outChannel.write(fv)
      }
      else {
        outChannel.write(inChannel.read())
      }
    }
  }
}