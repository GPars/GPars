package c5
 
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

One2OneChannel QP2Q = Channel.createOne2One()
One2OneChannel Q2QC = Channel.createOne2One()
One2OneChannel QC2Q = Channel.createOne2One()

def testList = [ new QProducer ( put: QP2Q.out(), 
		                         iterations: 50,
		                         delay: 0),
                 new Queue ( put: QP2Q.in(), 
                		     get: QC2Q.in(), 
                		     receive: Q2QC.out(), 
                		     elements: 5 ),
                 new QConsumer ( get: QC2Q.out(), 
                		         receive: Q2QC.in(), 
                		         delay: 0 )
               ]
new PAR ( testList ).run()
