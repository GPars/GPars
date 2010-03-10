package c6

import org.jcsp.lang.*

class ConsumeHW implements CSProcess {
  
  def ChannelInput inChannel
  def message
  
  void run() {
    def first = inChannel.read()
    def second = inChannel.read()    
    message = "${first} ${second}!!!"    
    println message
  }
}

