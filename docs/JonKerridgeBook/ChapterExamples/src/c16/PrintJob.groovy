package c16
 
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*

class PrintJob  implements Serializable{

  def int userId
  def NetChannelLocation useLocation
}