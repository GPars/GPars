package c3

import org.jcsp.groovy.plugAndPlay.*

import org.jcsp.lang.*
import org.jcsp.groovy.*

One2OneChannel N2P = Channel.createOne2One()

def testList = [ new GNumbers    ( outChannel: N2P.out() ),
                 new GPrint      ( inChannel: N2P.in(),
                                   heading : "Numbers" )
               ]
new PAR ( testList ).run()                                               