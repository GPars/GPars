package c16
 
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

def user = Ask.Int ("User Number ? ", 1, 999)

Node.info.setDevice(null)

Node.getInstance().init(new TCPIPNodeFactory ())

def pRequest = CNS.createAny2Net ("REQUEST")
def pRelease = CNS.createAny2Net ("RELEASE")

new PAR ( [ new PrintUser ( printerRequest: pRequest, 
                            printerRelease: pRelease, 
                            userId : user
                          )
          ] ).run()

