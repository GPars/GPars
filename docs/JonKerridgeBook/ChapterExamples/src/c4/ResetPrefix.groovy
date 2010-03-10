package c4

import org.jcsp.lang.*
import org.jcsp.groovy.*

class ResetPrefix implements CSProcess {
  
  def int prefixValue = 0
  def ChannelOutput outChannel
  def ChannelInput  inChannel
  def ChannelInput  resetChannel
  
  void run () {
    def guards = [ resetChannel, inChannel  ]
    def alt = new ALT ( guards )
    outChannel.write(prefixValue)
    while (true) {
      def index = alt.priSelect()
      if (index == 0 ) {    // resetChannel input
        def resetValue = resetChannel.read()
        def inputValue = inChannel.read()     // read the inChannel and ignore it
        outChannel.write(resetValue)
      }
      else {    //inChannel input only
        def inputValue = inChannel.read()     // and send it on through the system
        outChannel.write(inputValue)        
      }
    }
  }
}
