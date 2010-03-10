package c18
 
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory())

def int nodeId = Ask.Int ("Node identification? ", 1, 9)

def toRoot = CNS.createAny2Net("toRoot")

def processNode = new TripNode ( toRoot: toRoot,
                                  nodeId: nodeId) 


new PAR ([processNode]).run()



  
