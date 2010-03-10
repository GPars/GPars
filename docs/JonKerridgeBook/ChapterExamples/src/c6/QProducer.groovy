package c6

import org.jcsp.lang.*

class QProducer implements CSProcess {
  
  def ChannelOutput put
  def int iterations = 100
  def delay = 0
  
  def sequence = []
  
  void run () {
	def timer = new CSTimer()
    //println  "QProducer has started"
    for ( i in 1 .. iterations ) {
      put.write(i)
      timer.sleep (delay)
      sequence = sequence << i
    }
	put.write(null)
  }
}
