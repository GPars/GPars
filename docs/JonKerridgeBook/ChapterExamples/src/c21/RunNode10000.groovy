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

// pList and vList must be identical in content

def pList = [ new Type1Process()] 
def vList = [ new Type1Process()]
              
def processList = new NodeProcess ( nodeId: 100000,
                                     toGathererName: toGathererName,
                                     toDataGenName: toDataGenName,
                                     processList: pList,
                                     vanillaList: vList
                                   )

new PAR ([ processList]).run()
