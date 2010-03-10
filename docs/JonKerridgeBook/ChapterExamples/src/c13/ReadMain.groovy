// GPars (formerly GParallelizer)
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package c13

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory())

def readId = Ask.Int("Reader process ID? ", 0, 4)

println "Read Process ${readId} is creating its Net channels "

//NetChannelInput 
def fromDB = CNS.createNet2One("DB2P" + readId)  // the net channel from the database
println "  created DB2P${readId}"

//NetChannelOutput 
def toDB = CNS.createOne2Net("P2DB" + readId) // the net channel to the database
println "  created P2DB${readId}"
println "Read Process ${readId} has created its Net channels "

def pList = [new Read(id: readId, r2db: toDB, db2r: fromDB)]

new PAR(pList).run()