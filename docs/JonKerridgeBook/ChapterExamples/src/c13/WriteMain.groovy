package c13

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory ())

    
def writeId = Ask.Int ("Writer process ID? ", 0, 4) 
def readers = Ask.Int ("Number of reader processes ? ", 1, 5) 

println "Write Process ${writeId} is creating its Net channels "

//NetChannelInput 
def fromDB = CNS.createNet2One("DB2P" + (readers + writeId))  // the net channel from the database
println "    created DB2P${readers+writeId}"

//NetChannelOutput 
def toDB = CNS.createOne2Net("P2DB" + (readers + writeId)) // the net channel to the database
println "    created P2DB${readers+writeId}"

println "Write Process ${writeId} has created its Net channels "

def pList = [ new Write ( id:writeId, w2db:toDB, db2w:fromDB ) ]

new PAR (pList).run()