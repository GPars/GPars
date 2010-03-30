package c3 

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

One2OneChannel N2I = Channel.createOne2One()
One2OneChannel I2D = Channel.createOne2One()
One2OneChannel D2P = Channel.createOne2One()

def testList = [ new GNumbers ( outChannel: N2I.out() ),
                 new GIntegrate ( inChannel: N2I.in(), 
                		          outChannel: I2D.out() ),
                 new DifferentiateNeg ( inChannel:I2D.in(), 
                		                outChannel:D2P.out() ),
                 new GPrint ( inChannel: D2P.in(), heading: "Differentiated Numbers" )
               ]

new PAR ( testList ).run()                          