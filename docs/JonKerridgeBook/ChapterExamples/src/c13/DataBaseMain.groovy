package c13

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

int nReaders = Ask.Int ( "Number of Readers ? ", 1, 5)
int nWriters = Ask.Int ( "Number of Writers ? ", 1, 5)

Node.getInstance().init(new TCPIPNodeFactory ())

def toDB = new ChannelInputList()
def fromDB = new ChannelOutputList()
println "Creating reader network channels"
for ( i in 0 ..< nReaders  ) {
  toDB.append ( CNS.createNet2One ( "P2DB" + i ) )
  println "DBM: created P2DB${i}at ${i}"
  fromDB.append ( CNS.createOne2Net ( "DB2P" + i ) )
  println "DBM: created DB2P${i} at ${i}"
}
println "Creating writer network channels"

for ( i in 0 ..< nWriters  ) {
  toDB.append ( CNS.createNet2One ( "P2DB" + (i + nReaders) ) )
  println "DBM: created P2DB${i+nReaders} at ${(i+nReaders)}"
  fromDB.append ( CNS.createOne2Net ( "DB2P" + (i + nReaders) ) )
  println "DBM: created DB2P${i+nReaders} at ${(i+nReaders)}"
}
println "DBM: Creating database process list"

def pList = [ new DataBase ( inChannels: toDB,
                             outChannels: fromDB,
                             readers: nReaders,
                             writers: nWriters ) ]
println "DBM: Running Database"

new PAR (pList).run()                              