package c21
 
   
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*

Node.info.setDevice(null)

Node.getInstance().init(new TCPIPNodeFactory())

def toGatherer = "NodesToGatherer"

def fromNodes = CNS.createNet2One(toGatherer)

def processList = new Gatherer ( fromNodes: fromNodes )

new PAR ([ processList]).run()
