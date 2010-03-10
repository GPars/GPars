package c12.canteen

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Clock implements CSProcess {
  
  def ChannelOutput toConsole
  
  def void run() {

    def tim = new CSTimer()
    def tick = 0

    while (true) {
      toConsole.write ("Time: ${tick}\n")
      tim.sleep (1000)
      tick = tick + 1
    }

  }

}
