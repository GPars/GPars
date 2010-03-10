package c3 

import org.jcsp.groovy.plugAndPlay.GPrint
import org.jcsp.lang.*
import org.jcsp.groovy.*

One2OneChannel S2P = Channel.createOne2One()

def testList = [ new GSquares ( outChannel: S2P.out() ),
                 new GPrint   ( inChannel: S2P.in(),
                                heading : "Squares" )
               ]

new PAR ( testList ).run()       