package c2
        
import phw.util.*
import org.jcsp.lang.*
  
class Producer implements CSProcess {
  
  def ChannelOutput outChannel
  
  void run() {
    def i = 1000
    while ( i > 0 ) {
      i = Ask.Int ("next: ", -100, 100)
      outChannel.write (i)
    }
  }
}