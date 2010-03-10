package c2

import org.jcsp.lang.*
import org.jcsp.groovy.*

One2OneChannel connect = Channel.createOne2One()

def processList = [ 
                    new ProduceHW ( outChannel: connect.out() ),
                    new ConsumeHW ( inChannel: connect.in() )
                  ]
new PAR (processList).run()                   