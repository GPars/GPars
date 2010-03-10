package c9
  
import org.jcsp.lang.*
import org.jcsp.groovy.*

class UniformlyDistributedDelay implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel
  def int minTime = 100
  def int maxTime = 1000

  
  def void run () {
    def timer = new CSTimer()
    def rng = new Random()
    while (true) {
      def v = inChannel.read().copy()
      def delay = minTime + rng.nextInt ( maxTime - minTime )
      timer.sleep (delay)
      outChannel.write( v )
    }
  }
}  