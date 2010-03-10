package c18

import org.jcsp.lang.*
import org.jcsp.net.*

class TripNode implements CSProcess{
  
  def ChannelOutput toRoot
  def int nodeId
  
  def One2OneChannel N2A = Channel.createOne2One()
  def One2OneChannel A2N = Channel.createOne2One()  

  void run() {
    def ChannelInput toAgentInEnd = N2A.in()
    def ChannelInput fromAgentInEnd = A2N.in()
    def ChannelOutput toAgentOutEnd = N2A.out()
    def ChannelOutput fromAgentOutEnd = A2N.out()
    
    def agentInputChannel = NetChannelEnd.createNet2One()
    toRoot.write ( agentInputChannel.getChannelLocation())
    def theAgent = agentInputChannel.read()
    theAgent.connect ( [fromAgentOutEnd, toAgentInEnd] )
    def agentManager = new ProcessManager (theAgent)
    agentManager.start()
    def currentList = fromAgentInEnd.read()
    currentList << nodeId
    toAgentOutEnd.write (currentList)
    agentManager.join()
  }

}