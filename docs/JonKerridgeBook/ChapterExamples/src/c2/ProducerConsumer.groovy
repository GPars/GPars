package c2
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

One2OneChannel connect = Channel.createOne2One()

def processList = [ new Producer ( outChannel: connect.out() ),
                    new Consumer ( inChannel: connect.in() )
                  ]
new PAR (processList).run()                   