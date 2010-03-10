package c20
 
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory())

def int nodeId = 4
def int sentMessages = 150
def int nodes = 4

def fromRingName = "ring4" 
def toRingName = "ring0"

println " Node $nodeId: connection from $fromRingName to $toRingName "

def fromRing = CNS.createNet2One(fromRingName)
def toRing = CNS.createAny2Net(toRingName)

def processNode = new AgentElement ( fromRing: fromRing,
                                     toRing: toRing,
                                     element: nodeId,
                                     iterations: sentMessages,
                                     nodes: nodes) 

new PAR ([ processNode]).run()
