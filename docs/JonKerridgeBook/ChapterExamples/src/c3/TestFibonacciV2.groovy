package c3

import org.jcsp.groovy.plugAndPlay.*

import org.jcsp.lang.*
import org.jcsp.groovy.*

One2OneChannel F2P = Channel.createOne2One()

def testList = [ new FibonacciV2 ( outChannel: F2P.out() ),
                 new GPrint      ( inChannel: F2P.in(), 
                                   heading: "Fibonacci V2")
               ]

new PAR ( testList ).run()                          