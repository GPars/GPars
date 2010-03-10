package c3

import org.jcsp.groovy.plugAndPlay.GPCopy
import org.jcsp.groovy.plugAndPlay.GNumbers
import org.jcsp.groovy.plugAndPlay.GIntegrate
import org.jcsp.groovy.plugAndPlay.GPrefix
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

One2OneChannel [] connect = Channel.createOne2One(5)
One2OneChannel [] outChans = Channel.createOne2One(3)

def printList = new ChannelInputList ( outChans )

def titles = [ "n", "int", "sqr" ]

def testList =  [ new GNumbers   ( outChannel: connect[0].out() ),
                  new GPCopy     ( inChannel: connect[0].in(),
                                   outChannel0: connect[1].out(),
                                   outChannel1: outChans[0].out() ),
                  new GIntegrate ( inChannel: connect[1].in(), 
                                   outChannel: connect[2].out() ),
                  new GPCopy     ( inChannel: connect[2].in(),
                                   outChannel0: connect[3].out(),
                                   outChannel1: outChans[1].out() ),
                  new GPairs     ( inChannel: connect[3].in(), 
                                   outChannel: connect[4].out() ),
                  new GPrefix    ( prefixValue: 0, 
                                   inChannel: connect[4].in(), 
                                   outChannel: outChans[2].out() ),
                  new GParPrintListRead  ( inChannels: printList, 
                                           headings: titles )
                ]
new PAR ( testList ).run()                                