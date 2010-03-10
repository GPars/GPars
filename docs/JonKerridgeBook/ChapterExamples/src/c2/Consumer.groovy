package c2
    
import org.jcsp.lang.*
    
class Consumer implements CSProcess {
  
  def ChannelInput inChannel
  
  void run() {
    def i = 1000
    while ( i > 0 ) {
      i = inChannel.read()
      println "the input was : ${i}"
    }
    println "Finished"
  }
}

