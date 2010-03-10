package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*
import c12.canteen.*


Node.getInstance().init(new TCPIPNodeFactory ())

def cooked = CNS.createNet2One ("COOKED")
def getOne = CNS.createNet2One ("GETONE")
def gotOne = CNS.createOne2Net ("GOTONE")

def processList = [
  new ClockedQueuingServery(service:getOne, deliver:gotOne, supply:cooked)
  ]

new PAR ( processList ).run()     
