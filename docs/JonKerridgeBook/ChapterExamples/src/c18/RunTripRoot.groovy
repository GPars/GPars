package c18 

import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory())

def String initialValue = Ask.string ( "Initial List Value ? ")
def int nodes = Ask.Int ("Number of nodes? ", 1, 9)

def fromNodes = CNS.createNet2One("toRoot")

println " Root: input channel created "

def rootNode = new TripRoot ( fromNodes: fromNodes, 
                               nodes: nodes,
                               initialValue: initialValue )

new PAR ( [rootNode] ).run()
