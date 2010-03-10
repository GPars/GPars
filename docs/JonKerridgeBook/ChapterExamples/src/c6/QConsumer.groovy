package c6

import org.jcsp.lang.*

class QConsumer implements CSProcess {
  
  def ChannelOutput get
  def ChannelInput receive
  def long delay = 0
  
  def outSequence = []
  
  void run () {
    def timer = new CSTimer()
    //println  "QConsumer has started"
    def running = true
    while (running) {
      get.write(1)  
      def v = receive.read()
      //println "QConsumer has read ${v}"
      timer.sleep (delay)
      if ( v != null) {
        outSequence = outSequence << v
      } else {
    	  running = false
      }
    }
  }
}

