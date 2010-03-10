package c14

import org.jcsp.lang.*
import org.jcsp.groovy.*

class MouseBufferPrompt implements CSProcess{
  def ChannelOutput returnPoint
  def ChannelOutput getPoint
  def ChannelInput receivePoint  
  def Barrier setUpBarrier

  void run () {
    setUpBarrier.sync()
    while (true) {
      getPoint.write( 1 )
      def point = receivePoint.read()
      returnPoint.write( point )
    }    
  }

}