package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelOutput
import org.jcsp.lang.One2OneChannel
import org.jcsp.lang.Channel
import groovyx.gpars.csp.PAR

class GSquares implements CSProcess {
  
  def ChannelOutput outChannel
  
  void run () {

    One2OneChannel N2I = Channel.createOne2One()
    One2OneChannel I2P = Channel.createOne2One()

    def testList =  [ new GNumbers   ( outChannel: N2I.out() ),
                      new GIntegrate ( inChannel: N2I.in(), 
                                       outChannel: I2P.out() ),
                      new GPairs     ( inChannel: I2P.in(), 
                                       outChannel: outChannel ),
                    ]
    new PAR ( testList ).run()  
  }
}
                              