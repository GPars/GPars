package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory ())

def comms = CNS.createNet2Any ("comms")

def pList = (0 .. 5).collect{i -> new Get ( inChannel: comms, id: i ) }

new PAR ( pList ).run()