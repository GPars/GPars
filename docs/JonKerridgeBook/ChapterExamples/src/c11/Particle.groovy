package c11
 
import org.jcsp.lang.*

class Particle implements CSProcess {
  def ChannelOutput sendPosition
  def ChannelInput getPosition
  def int x = 100         // initial x location
  def int y = 100         // initial y location
  def long delay = 200    // delay between movements
  def int id
  def int temperature = 25 // in range 10 to 50
  
  void run() {
    def timer = new CSTimer()
    def rng = new Random()
    def p = new Position ( id: id, px: x, py: y, temperature: temperature )
    while (true) {
      p.lx = p.px + rng.nextInt(p.temperature) - ( p.temperature / 2 )
      p.ly = p.py + rng.nextInt(p.temperature) - ( p.temperature / 2 )
      sendPosition.write ( p )
      p = ( (Position)getPosition.read() ).copy()   // p now has updated position
      //println " " + p.toString()
      timer.sleep ( delay )
    }
  }
}

