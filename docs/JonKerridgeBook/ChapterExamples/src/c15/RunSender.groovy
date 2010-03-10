package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

def v= Ask.string ("Sender identity string ? ")

Node.getInstance().init(new TCPIPNodeFactory ())

def comms = CNS.createAny2Net ("comms")

def pList = [ new Sender ( outChannel: comms, id: v ) ]

new PAR ( pList ).run()
