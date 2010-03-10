package c13

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory ())
    
def readId = Ask.Int ("Reader process ID? ", 0, 4) 

println "Read Process ${readId} is creating its Net channels "

//NetChannelInput 
def fromDB = CNS.createNet2One("DB2P" + readId)  // the net channel from the database
println "  created DB2P${readId}"

//NetChannelOutput 
def toDB = CNS.createOne2Net("P2DB" + readId) // the net channel to the database
println "  created P2DB${readId}"
println "Read Process ${readId} has created its Net channels "

def pList = [ new Read ( id:readId, r2db: toDB, db2r: fromDB ) ]

new PAR (pList).run()