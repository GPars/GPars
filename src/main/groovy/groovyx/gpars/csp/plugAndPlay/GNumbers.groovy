package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelOutput
import org.jcsp.lang.One2OneChannel
import org.jcsp.lang.Channel
import groovyx.gpars.csp.PAR

class GNumbers implements CSProcess {
  
  def ChannelOutput outChannel
  
  void run() {
    
    One2OneChannel a = Channel.createOne2One()
    One2OneChannel b = Channel.createOne2One()
    One2OneChannel c = Channel.createOne2One()
    
    def numbersList =  [ new GPrefix    ( prefixValue: 0, 
                                          inChannel: c.in(), 
                                          outChannel: a.out() ),
                         			new GPCopy     ( inChannel: a.in(), 
                                          outChannel0: outChannel, 
                                          outChannel1: b.out() ),
                         			new GSuccessor ( inChannel: b.in(), 
                                          outChannel: c.out()) 
                      			 ]
    new PAR ( numbersList ).run()
  }
}
                         
                         