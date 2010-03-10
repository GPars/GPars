package c15

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

def v = Ask.Int ("Get id? ", 1, 9)

Node.getInstance().init(new TCPIPNodeFactory ())

def comms = CNS.createNet2Any ("comms")

def pList = [ new Get ( inChannel: comms , id: v ) ]

new PAR ( pList ).run()