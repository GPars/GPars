package c7

import org.jcsp.lang.*
import org.jcsp.groovy.*

class BadP implements CSProcess {
  def ChannelInput inChannel
  def ChannelOutput outChannel
  
  def void run() {
    println "BadP: Starting"
    while (true) {
      println "BadP: outputting"
      outChannel.write(1)
      println "BadP: inputting"
      def i = inChannel.read()
      println "BadP: looping"
    }
  }
}

      