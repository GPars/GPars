package c3

import org.jcsp.groovy.plugAndPlay.*

import org.jcsp.lang.*
import org.jcsp.groovy.*

One2OneChannel F2P = Channel.createOne2One()

def testList = [ new FibonacciV1 ( outChannel: F2P.out() ),
                 new GPrint      ( inChannel: F2P.in(), 
                                   heading: "Fibonacci V1")
               ]

new PAR ( testList ).run()                          