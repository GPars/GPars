package c21
 
  
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import phw.util.*

Node.info.setDevice(null)

Node.getInstance().init(new TCPIPNodeFactory())

def toDataGenName = "NodesToDataGen"
def toGathererName = "NodesToGatherer"

def pList = [ ]
def vList = [ ]
              
def processList = new NodeProcess ( nodeId: 600000,
                                     toGathererName: toGathererName,
                                     toDataGenName: toDataGenName,
                                     processList: pList,
                                     vanillaList: vList
                                   )

new PAR ([ processList]).run()
