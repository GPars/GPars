package c9
  
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

One2OneChannel eg2h = Channel.createOne2One()
One2OneChannel h2udd = Channel.createOne2One()
One2OneChannel udd2prn = Channel.createOne2One()


def eventTestList = [ 
      new EventGenerator ( source: 1, 
                           initialValue: 100, 
                           iterations: 100, 
                           outChannel: eg2h.out(), 
                           minTime: 100, 
                           maxTime:200 ),
      new EventHandler ( inChannel: eg2h.in(), 
                         outChannel: h2udd.out() ),
      new UniformlyDistributedDelay ( inChannel:h2udd.in(), 
                                      outChannel: udd2prn.out(), 
                                      minTime: 1000, 
                                      maxTime: 2000 ), 
      new GPrint ( inChannel: udd2prn.in(),
    		        heading : "Event Output",
    		        delay: 0)
      ]
new PAR ( eventTestList ).run() 
             