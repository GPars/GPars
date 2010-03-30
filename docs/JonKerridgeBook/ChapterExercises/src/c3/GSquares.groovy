package c3 

import org.jcsp.groovy.plugAndPlay.*
import org.jcsp.lang.*
import org.jcsp.groovy.*

class GSquares implements CSProcess {
  
  def ChannelOutput outChannel
  
  void run () {

    One2OneChannel N2I = Channel.createOne2One()
    One2OneChannel I2P = Channel.createOne2One()

    def testList =  [ new GNumbers   ( outChannel: N2I.out() ),
                      new GIntegrate ( inChannel: N2I.in(), 
                                       outChannel: I2P.out() ),
                      // you will need to modify thi twice
                      //first modification is to insert a constructor for GSPairsA
                      // then run the network using TestGSCopy
                      //second modification replace the constructor for GSPairsA with GSPairsB
                      // then run the network again using TestGSCopy
                      // you will then be able to compare the behaviour and also to
                      // explain why this happens!
                    ]
    new PAR ( testList ).run()  
  }
}
                              