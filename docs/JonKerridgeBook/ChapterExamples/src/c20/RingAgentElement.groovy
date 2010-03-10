package c20
  
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class RingAgentElement implements CSProcess {
  
  def ChannelInput fromRing
  def ChannelOutput toRing
  def ChannelInput fromSender
  def ChannelInput fromStateManager
  def ChannelOutput toQueue
  def int element
  
  void run() {
    def One2OneChannel N2A = Channel.createOne2One()
    def One2OneChannel A2N = Channel.createOne2One()  

    def ChannelInput toAgentInEnd = N2A.in()
    def ChannelInput fromAgentInEnd = A2N.in()
    def ChannelOutput toAgentOutEnd = N2A.out()
    def ChannelOutput fromAgentOutEnd = A2N.out()
    
    def stopper = new StopAgent ( homeNode: element, 
                                   previousNode: element - 1, 
                                   initialised: false)
    def restarter = new RestartAgent ( homeNode: element, 
                                        previousNode: element - 1,
                                        firstHop: true)
    
    def NetChannelLocation originalToRing = toRing.getChannelLocation()
    
    def failedList = [ ]
    
    def RING = 0
    def SENDER= 1
    def MANAGER = 2
    def ringAlt = new ALT ( [ fromRing, fromSender, fromStateManager ] )
    def preCon = new boolean[3]
    preCon[RING] = true
    preCon[SENDER] = true
    preCon[MANAGER] = true   // always the case
    def emptyPacket = new RingPacket ( source: -1, destination: -1 , value: -1 , full: false)
    def localBuffer = new RingPacket()
    def localBufferFull = false
    def restartBuffer = null
    def restarting = false
    def stopping = false
    toRing.write ( emptyPacket )
    while (true) {
      def index = ringAlt.select(preCon)
      switch (index) {
        case RING:
          def ringBuffer = fromRing.read()
          if ( ringBuffer instanceof RingPacket) {
            if ( ringBuffer.destination == element ) {  
              // packet for this node; full should be true
              toQueue.write(ringBuffer)
              // now write either stopper, restarter, localBuffer or empty packet to ring
              if (stopping) {
                stopping = false
                toRing.write(stopper)
              }
              else {
                if (restarting) {
                  restarting = false
                  toRing.write(restartBuffer)
                }
                else {
                  if ( localBufferFull ) {
                    toRing.write ( localBuffer )
                    preCon[SENDER] = true          // allow another packet from Sender
                    localBufferFull = false
                  } 
                  else {
                    toRing.write ( emptyPacket )
                  }
                }
              }              
            }
            else {
              if ( ringBuffer.full ) {
                // packet for onward transmission to another element
                toRing.write ( ringBuffer )
              }
              else {
                // have received an empty packet
                // now write either stopper, restarter, localBuffer or empty packet to ring
                if (stopping) {
                  stopping = false
                  toRing.write(stopper)
                }
                else {
                  if (restarting) {
                    restarting = false
                    toRing.write(restartBuffer)
                  }
                  else {
                    if ( localBufferFull ) {
                      toRing.write ( localBuffer )
                      preCon[SENDER] = true          // allow another packet from Sender
                      localBufferFull = false
                    } 
                    else {
                      toRing.write ( emptyPacket )
                    }
                  }
                }
              }
            }

          }
          else {
            if (ringBuffer instanceof StopAgent) {
              def theAgent = ringBuffer
              theAgent.connect ( [fromAgentOutEnd, toAgentInEnd] )
              def agentManager = new ProcessManager (theAgent)
              agentManager.start()
              def failedNode = fromAgentInEnd.read()
              failedList << failedNode
              println "Node $element: stopping failed list now $failedList"
              def targetNode = fromAgentInEnd.read()
              def alreadyInitialised = fromAgentInEnd.read()
              if ( ! alreadyInitialised ) {
                toAgentOutEnd.write (fromRing.getChannelLocation())
              }
              if (element == targetNode) {
                // got to node that needs to be changed
                toAgentOutEnd.write(true)
                def NetChannelLocation revisedToRing = fromAgentInEnd.read()
                toRing = NetChannelEnd.createAny2Net(revisedToRing)
                agentManager.join()
                theAgent.disconnect()
                println "Node $element: stopping has redirected toRing"
                // no need to send agent any further its got to its target
                // ring has lost a node hence do not send an empty packet
              }
              else {
                toAgentOutEnd.write(false)
                agentManager.join()
                theAgent.disconnect()
                toRing.write(theAgent)
                println "Node $element: stopping has passed agent on to next node"
              }         
            }
            else {
              // must be instance of RestartAgent
              def theAgent = ringBuffer
              theAgent.connect ( [fromAgentOutEnd, toAgentInEnd] )
              def agentManager = new ProcessManager (theAgent)
              agentManager.start()
              def firstHop = fromAgentInEnd.read()
              def resumedNode = fromAgentInEnd.read()
              failedList = failedList - [resumedNode]
              println "Node $element: restarting failed list now $failedList"
              def targetNode = fromAgentInEnd.read()
              if (firstHop) {
                agentManager.join()
                theAgent.disconnect()
                restartBuffer = theAgent
                restarting = true
              }
              else {
                if (element == targetNode) {                
                  toRing = NetChannelEnd.createAny2Net (originalToRing)
                  println "Node $element: restarting has redirected toRing"
                  agentManager.join()
                  theAgent.disconnect()
                  // no need to send agent any further its got to its target
                  // but the node has been reinstated hence need another packet on ring
                  toRing.write ( emptyPacket )
                }
                else {
                  agentManager.join()
                  theAgent.disconnect()
                  toRing.write(theAgent)
                  println "Node $element: restarting has passed agent on to next node"
                }         
                
              }
            }
          }
          break
        case SENDER:  
          localBuffer = fromSender.read()
          // test to see if destination is not in failedList
          if ( ! failedList.contains(localBuffer.destination) ) {
            preCon[SENDER] = false             // stop any more Sends until buffer is emptied
            localBufferFull = true
          }
          // otherwise we just throw away the message to a stopped node and it is lost forever!
          break
        case MANAGER:
          // receiver has restarted
          def state = fromStateManager.read() 
          if (state == "STOP") {
            //will write the stopper agent when an empty packet is recieved
            stopping = true
            restarting = false
          }
          else {
            toRing.write ( restarter )
            // this is a stopped node and thus this is the only thing it can do
            // but it will be putting an extra packet onto the ring
            // this is why the first Hop processing has to be done if a node
            // gets a restart agent
          }
          break
      }  // end switch
    }
  }
}
 