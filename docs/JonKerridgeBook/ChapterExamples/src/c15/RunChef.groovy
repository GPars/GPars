package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*
import c12.canteen.*

Node.getInstance().init(new TCPIPNodeFactory ())

def cooked = CNS.createOne2Net ("COOKED")
    
def processList = [ new Kitchen ( supply: cooked) ]

new PAR ( processList ).run()     