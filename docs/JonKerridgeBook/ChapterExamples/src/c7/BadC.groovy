package c7

import org.jcsp.lang.*
import org.jcsp.groovy.*

class BadC implements CSProcess {
  def ChannelInput inChannel
  def ChannelOutput outChannel
  
  def void run() {
    println "BadC: Starting"
    while (true) {
      println "BadC: outputting"
      outChannel.write(1)
      println "BadC: inputting"
      def i = inChannel.read()
      println "BadC: looping"
    }
  }
}

      