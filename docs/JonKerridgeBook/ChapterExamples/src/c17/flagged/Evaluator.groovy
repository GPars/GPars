package c17.flagged

import org.jcsp.groovy.*
import org.jcsp.lang.*

class Evaluator implements CSProcess {
  
  def ChannelInput inChannel

  void run() {
    while (true) {
      def v = inChannel.read()
      def ok = (v.c == (v.a +v.b))
      println "Evaluation: ${ok} from " + v.toString()
    }    
  }
}