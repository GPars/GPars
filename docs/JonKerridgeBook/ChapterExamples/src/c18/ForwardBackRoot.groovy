package c18

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*

class ForwardBackRoot implements CSProcess{
  
  def ChannelInput inChannel
  def ChannelOutput outChannel
  def int iterations
  def String initialValue
  def NetChannelInput backChannel
  
  void run() {
    def One2OneChannel N2A = Channel.createOne2One()
    def One2OneChannel A2N = Channel.createOne2One()  
    def ChannelInput toAgentInEnd = N2A.in()
    def ChannelInput fromAgentInEnd = A2N.in()
    def ChannelOutput toAgentOutEnd = N2A.out()
    def ChannelOutput fromAgentOutEnd = A2N.out()

    def backChannelLocation = backChannel.getChannelLocation()
    
    def theAgent = new ForwardBackAgent( results: [initialValue],
                                          backChannel: backChannelLocation)
    
    def rootAlt = new ALT ( [inChannel, backChannel])
    outChannel.write(theAgent)
    def i = 1
    def rootValue = -1
    def running = true
    while ( running) {
      def index = rootAlt.select()
      switch (index) {
        case 0:		// agent has returned
          theAgent = inChannel.read()
          theAgent.connect ( [fromAgentOutEnd, toAgentInEnd] )
          def agentManager = new ProcessManager (theAgent)
          agentManager.start()
          def forwardLocation = backChannel.read()
          def forwardChannel = NetChannelEnd.createOne2Net(forwardLocation)
          forwardChannel.write (rootValue)
          rootValue = rootValue - 1
          def returnedResults = fromAgentInEnd.read()
          println "Root: Iteration: $i is $returnedResults "    
          returnedResults << "end of " + i
          toAgentOutEnd.write (returnedResults)
          def backValue = backChannel.read()
          agentManager.join()
          theAgent.disconnect()
          i = i + 1
          if (i <= iterations) {
            outChannel.write(theAgent)
          }
          else {
            running = false
          }
          break
        case 1:
          def forwardLocation = backChannel.read()
          def forwardChannel = NetChannelEnd.createOne2Net(forwardLocation)
          forwardChannel.write (rootValue)
          rootValue = rootValue - 1
          def backValue = backChannel.read()
          println "Root: During Iteration $i: received $backValue"
          break
      }
    }
  }

}