package c9 
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class EventOWBuffer implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelInput getChannel
  def ChannelOutput outChannel
  
  def void run () {
    def owbAlt = new ALT ( [inChannel, getChannel] )
    def INCHANNEL = 0
    def GETCHANNEL = 1
    def preCon = new boolean[2]
    preCon[INCHANNEL] = true
    preCon[GETCHANNEL] = false
    def e = new EventData ()
    def missed = -1
    while (true) {
      def index = owbAlt.priSelect ( preCon )
      switch ( index ) {
        case INCHANNEL:
          e = inChannel.read().copy()
          missed = missed + 1
          e.missed = missed
          preCon[GETCHANNEL] = true
          break
        case GETCHANNEL:
          def s = getChannel.read()
          outChannel.write ( e )
          missed = -1                 // reset the missed count field
          preCon[GETCHANNEL] = false
          break
      }  // end switch
    }  // end while
  }  // end run
}