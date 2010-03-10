package c14

import org.jcsp.lang.*
import org.jcsp.groovy.*
import java.awt.Point
import java.awt.event.*

class MouseBufferPreCon implements CSProcess{
  def ChannelInput mouseEvent
  def ChannelInput getClick
  def ChannelOutput sendPoint

  void run() {
    def mouseBufferAlt = new ALT ( [ getClick, mouseEvent ] )
    def preCon = new boolean [2]
    def EVENT = 1
    def GET = 0
    preCon[EVENT]= true
    preCon[GET] = false
    def point
    while (true) {
      switch (mouseBufferAlt.select(preCon)) {
        case GET:
          getClick.read()
          sendPoint.write(point)
          preCon[GET] = false
          break
        case EVENT:
          def mEvent = mouseEvent.read()
          if ( mEvent.getID() == MouseEvent.MOUSE_PRESSED) {
            preCon[GET] = true
            point = mEvent.getPoint()
          }
          break
      }
    }
  }
}