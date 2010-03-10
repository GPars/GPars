package c11
 
import org.jcsp.groovy.*

class Position implements JCSPCopy {
  def int id            // particle number
  def int lx            // current x location of particle
  def int ly            // current y location of particle
  def int px            // previous x location of particle
  def int py            // previous y location of particle
  def int temperature   // current working temperature
  
  def copy() {
    def p = new Position ( id: this.id,  
                           lx: this.lx, ly: this.ly, 
                           px: this.px, py: this.py,
                           temperature : this.temperature )
    return p
  }

  def String toString () {
    def s = "[Position-> " + id + ", " + lx + ", " + ly 
    s = s + ", " + px + ", " + py
    s = s + ", " + temperature + " ]"
    return s
  }
}
