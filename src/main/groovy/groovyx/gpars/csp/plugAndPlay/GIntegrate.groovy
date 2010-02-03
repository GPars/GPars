package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelOutput
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.One2OneChannel
import org.jcsp.lang.Channel
import groovyx.gpars.csp.PAR

class GIntegrate implements CSProcess {
  
  def ChannelOutput outChannel
  def ChannelInput  inChannel
  
  void run() {
    
    One2OneChannel a = Channel.createOne2One()
    One2OneChannel b = Channel.createOne2One()
    One2OneChannel c = Channel.createOne2One()
    
    def integrateList = [ new GPrefix ( prefixValue: 0, 
                                        outChannel: c.out(), 
                                        inChannel: b.in() ),
                          new GPCopy  ( inChannel: a.in(), 
                                        outChannel0: outChannel, 
                                        outChannel1: b.out() ),
                          new GPlus   ( inChannel0: inChannel, 
                                        inChannel1: c.in(), 
                                        outChannel: a.out() ) 
                        ]
    new PAR ( integrateList ).run()
    
  }
}
