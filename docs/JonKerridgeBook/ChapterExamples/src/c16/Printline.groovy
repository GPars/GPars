package c16
 
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*


class Printline implements Serializable {
  
  def int printKey
  def String line
  
}
