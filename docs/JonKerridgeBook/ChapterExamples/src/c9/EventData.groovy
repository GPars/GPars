package c9
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class EventData implements Serializable, JCSPCopy {
  
  def int source = 0
  def int data = 0
  def int missed = -1
  
  def copy() {
    def e = new EventData ( source: this.source, 
                             data: this.data, 
                             missed: this.missed )
    return e
  }
  
  def String toString() {
    def s = "EventData -> [source: "
    s = s + source + ", data: "
    s = s + data + ", missed: " 
    s = s + missed + "]"
    return s
  }
     
}


