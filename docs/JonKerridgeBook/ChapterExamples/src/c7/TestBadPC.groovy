package c7
 
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

One2OneChannel a = Channel.createOne2One()
One2OneChannel b = Channel.createOne2One()

def pList = [ new BadP ( inChannel: a.in(), outChannel: b.out() ),
              new BadC ( inChannel: b.in(), outChannel: a.out() )
            ]
new PAR (pList).run()