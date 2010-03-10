package c21
 
import org.jcsp.lang.*

class Gatherer implements CSProcess{
  def ChannelInput fromNodes

  void run() {
    while (true) {
      def d = fromNodes.read()
      println "Gathered from ${d.toString()}"      
    }    
  }

}