package c18
 
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory())

def int iterations = Ask.Int ("Number of Iterations ? ", 1, 9)
def String initialValue = Ask.string ( "Initial List Value ? ")

def fromRingName = "ring0"
def toRingName = "ring1"

def fromRing = CNS.createNet2One(fromRingName)
def toRing = CNS.createOne2Net(toRingName)

println " Root: connection from $fromRingName to $toRingName "

def rootNode = new Root ( inChannel: fromRing, 
                           outChannel: toRing,
                           iterations: iterations,
                           initialValue: initialValue )

new PAR ( [rootNode] ).run()
