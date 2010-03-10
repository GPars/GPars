package c20
   
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory())

def fromRingName = "ring0"
def toRingName = "ring1"

println " Node 0: connection from $fromRingName to $toRingName "

def fromRing = CNS.createNet2One(fromRingName)
def toRing = CNS.createAny2Net(toRingName)

def processNode = new AgentExtraElement ( fromRing: fromRing,
                                           toRing: toRing )

new PAR ([ processNode]).run()
