package groovyx.gpars.csp.util

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelOutput
import org.jcsp.lang.CSTimer
import groovyx.gpars.csp.ChannelInputList
import groovyx.gpars.csp.ALT


class TimedMultiplex implements CSProcess {
 
  def ChannelInputList inChannels
  def ChannelOutput outChannel
  def long timeout      // period during which process will run

  def void run () { 
    def timer = new CSTimer()
    def timerIndex = inChannels.size() 
    //def alt = new ALT( inChannels.plus ( timer ) )
    def alt = new ALT( inChannels + timer )
    timer.setAlarm( timer.read() + timeout )
    def running = true
    while (running) {
      def index = alt.select ()
      if (index == timerIndex) {
        running = false 
        println "TimedMultiplex has stopped"
      } else {
        outChannel.write (inChannels[index].read())
      }
    } 
  } 
}
