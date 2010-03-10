package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*
import c12.canteen.*

Node.getInstance().init(new TCPIPNodeFactory ())

def gotOne = CNS.createNet2Any ("GOTONE")
def getOne = CNS.createAny2Net ("GETONE")

    def philList = ( 0 .. 4 ).collect{
      i -> return new Philosopher(philosopherId:i, service:getOne, deliver:gotOne)}

new PAR ( philList ).run()     