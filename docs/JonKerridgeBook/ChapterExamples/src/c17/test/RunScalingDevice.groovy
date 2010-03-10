package c17.test

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*

Node.info.setDevice(null)

Node.getInstance().init(new TCPIPNodeFactory ())
  
NetChannelInput ordinaryInput = CNS.createNet2One("ordinaryInput")
NetChannelOutput scaledOutput = CNS.createOne2Net("scaledOutput")

new PAR(new ScalingDevice (inChannel: ordinaryInput, outChannel: scaledOutput) ).run()
 