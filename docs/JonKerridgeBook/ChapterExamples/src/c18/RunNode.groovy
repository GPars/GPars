package c18
 
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory())

def int nodeId = Ask.Int ("Node identification? ", 1, 9)
def Boolean last = Ask.Boolean ("Is this the last node? - ( y or n):")

def fromRingName = "ring" + nodeId

def toRingName = (last) ? "ring0" : "ring" + (nodeId + 1)
    
println " Node $nodeId: connection from $fromRingName to $toRingName "

def fromRing = CNS.createNet2One(fromRingName)
def toRing = CNS.createOne2Net(toRingName)

def processNode = new ProcessNode ( inChannel: fromRing,
                                     outChannel: toRing,
                                     nodeId: nodeId) 


new PAR ([ processNode]).run()



  
