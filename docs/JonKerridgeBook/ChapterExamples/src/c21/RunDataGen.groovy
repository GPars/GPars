package c21
  
    
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import phw.util.*

Node.info.setDevice(null)

Node.getInstance().init(new TCPIPNodeFactory())

def toDataGen = "NodesToDataGen"

def fromNodes = CNS.createNet2One(toDataGen)

def processList = new DataGenerator ( fromNodes: fromNodes, interval: 500 )

new PAR ([ processList]).run()
