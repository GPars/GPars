package c2

import org.jcsp.lang.*
import org.jcsp.groovy.*

One2OneChannel connect1 = Channel.createOne2One()
One2OneChannel connect2 = Channel.createOne2One()

def processList = [ new GenerateSetsOfThree ( outChannel: connect1.out() ),
                    new ListToStream ( inChannel: connect1.in(), 
                    		           outChannel: connect2.out() ),
                    new CreateSetsOfEight ( inChannel: connect2.in() )
                  ]
new PAR (processList).run()