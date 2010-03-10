package c18

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import org.jcsp.groovy.*

class TripRoot implements CSProcess{
  
  def ChannelInput fromNodes
  def String initialValue
  def int nodes
  
  void run() {
    
    def One2OneChannel N2A = Channel.createOne2One()
    def One2OneChannel A2N = Channel.createOne2One()  
    def ChannelInput toAgentInEnd = N2A.in()
    def ChannelInput fromAgentInEnd = A2N.in()
    def ChannelOutput toAgentOutEnd = N2A.out()
    def ChannelOutput fromAgentOutEnd = A2N.out()
    
    def tripList = [ fromNodes.getChannelLocation() ]
                     
    for ( i in 0 ..< nodes) {
      def nodeChannelLocation = fromNodes.read()
      tripList << nodeChannelLocation
    }
    def firstNodeLocation = tripList.get(nodes)
    def firstNodeChannel = NetChannelEnd.createOne2Net(firstNodeLocation)
    def theAgent = new TripAgent( tripList: tripList, 
                                   results: [initialValue],
                                   pointer: nodes)
    firstNodeChannel.write(theAgent)
    theAgent = fromNodes.read()
    theAgent.connect ( [fromAgentOutEnd, toAgentInEnd] )
    def agentManager = new ProcessManager (theAgent)
    agentManager.start()
    def returnedResults = fromAgentInEnd.read()
    println "TripRoot: has received $returnedResults "    
    toAgentOutEnd.write (returnedResults)
    agentManager.join()
    theAgent.disconnect()    
  }

}