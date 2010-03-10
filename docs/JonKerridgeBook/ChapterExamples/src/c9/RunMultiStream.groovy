package c9
 
   
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*
import phw.util.Ask

def sources = Ask.Int ("Number of event sources between 1 and 9 ? ", 1, 9)

minTimes = [ 10, 20, 30, 40, 50, 10, 20, 30, 40 ]
maxTimes = [ 100, 150, 200, 50, 60, 30, 60, 100, 80 ]             

One2OneChannel [] es2ep = Channel.createOne2One(sources)

ChannelInputList eventsList = new ChannelInputList (es2ep)

def sourcesList = ( 0 ..< sources).collect { i ->
                           new EventSource ( source: i+1, 
                                             outChannel: es2ep[i].out(),
                                             minTime: minTimes[i],
                                             maxTime: maxTimes[i] ) 
                           }

def eventProcess = new EventProcessing ( eventStreams: eventsList,
                                          minTime: 10,
                                          maxTime: 400 )

new PAR( sourcesList + eventProcess).run()