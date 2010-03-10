package c16
  
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

def spoolers = Ask.Int ("Number of spoolers ? ", 1, 9)

Node.info.setDevice(null)

Node.getInstance().init(new TCPIPNodeFactory ())

def pRequest = CNS.createNet2One ("REQUEST")
def pRelease = CNS.createNet2One ("RELEASE")

new PAR ( [ new PrintSpooler ( printerRequest: pRequest, 
                               printerRelease: pRelease, 
                               spoolers : spoolers  
                             )
          ] ).run()

