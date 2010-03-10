package c2

import org.jcsp.lang.*

class ConsumeHW implements CSProcess {
  
  def ChannelInput inChannel
  
  void run() {
    def first = inChannel.read()
    def second = inChannel.read()
    println "\n${first} ${second}!\n"
  }
}

