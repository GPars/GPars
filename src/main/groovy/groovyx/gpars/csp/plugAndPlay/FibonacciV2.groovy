package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelOutput
import org.jcsp.lang.One2OneChannel
import org.jcsp.lang.Channel
import groovyx.gpars.csp.PAR

class FibonacciV2 implements CSProcess {
  
  def ChannelOutput outChannel
  
  void run () {  

    One2OneChannel a  = Channel.createOne2One()
    One2OneChannel b  = Channel.createOne2One()
    One2OneChannel c  = Channel.createOne2One()
    One2OneChannel d  = Channel.createOne2One()

    def testList = [ new GPrefix ( prefixValue: 0, 
                                   inChannel: d.in(), 
                                   outChannel: a.out() ),
                     new GPrefix ( prefixValue: 1, 
                                   inChannel: c.in(), 
                                   outChannel: d.out() ),
                     new GPCopy  ( inChannel: a.in(), 
                                   outChannel0: b.out(), 
                                   outChannel1: outChannel ),
                     new GPairs  ( inChannel: b.in(), 
                                   outChannel: c.out() ), 
                   ]
    new PAR ( testList ).run()
  }
}
