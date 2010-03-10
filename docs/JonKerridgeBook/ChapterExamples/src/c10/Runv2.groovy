package c10

import org.jcsp.lang.*
import org.jcsp.groovy.*
import phw.util.*

println "starting v2...."

def nodes = Ask.Int ( "Number of Nodes ? - ", 3, 10 )

One2OneChannel[] ring = Channel.createOne2One(nodes+1)

def extra = new ExtraElementv2 ( fromRing: ring[0].in(),
                                  toRing:   ring[1].out())   


def elementList = (1..nodes).collect{i -> 
                                         def toR = (i+1)%(nodes+1)
                                         println "Creating Element: ${i} from  ${i} to ${toR}"
                                         return new Elementv2 ( fromRing: ring[i].in(),
                                                                 toRing: ring[toR].out(),
                                                                 element: i,
                                                                 nodes: nodes)
                                     }

new PAR (elementList + extra).run()