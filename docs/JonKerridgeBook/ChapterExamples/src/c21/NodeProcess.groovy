package c21
 
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*

class NodeProcess implements CSProcess {
  def int nodeId
  def String toGathererName
  def String toDataGenName
  def processList = null
  def vanillaList = null // these must be identical initially

  void run() {
    def toDataGen = CNS.createAny2Net(toDataGenName)
    def fromDataGen = NetChannelEnd.createNet2One()
    def agentVisitChannel= NetChannelEnd.createNet2One()
    def agentVisitChannelLocation = agentVisitChannel.getChannelLocation()
    def agentReturnChannel= NetChannelEnd.createNet2One()
    def agentReturnChannelLocation = agentReturnChannel.getChannelLocation()
    
    toDataGen.write( new DataGenList ( dgl: [ fromDataGen.getChannelLocation(), 
                                               agentVisitChannelLocation,
                                               nodeId] ) )
                                               
    def connectChannels = [ ]
    def typeOrder = [ ]
    def vanillaOrder = [ ]
    def currentSearches = [ ]
    def cp = 0
    
    if (processList != null) {                 
      for ( i in 0 ..< processList.size) {
        def processType = processList[cp].getClass().getName()
        def typeName = processType.substring(0, processType.indexOf("Process"))
        //println "NP-$nodeId: constructed with a process for $typeName as $cp"
        typeOrder << typeName 
        vanillaOrder << typeName 
        connectChannels[cp] = Channel.createOne2One()
        def pList = [connectChannels[cp].in(), nodeId, toGathererName]
        processList[cp].connect(pList)
        def pm = new ProcessManager(processList[cp])
        pm.start()
        cp = cp + 1
      }
    }
    //println "NP-$nodeId: initial typeOrder is $typeOrder"
    //println "NP-$nodeId: vanilla Order is $vanillaOrder"
    
    def NodeToInitialAgent = Channel.createOne2One()
    def NodeToVisitingAgent = Channel.createOne2One()
    def NodeFromVisitingAgent = Channel.createOne2One()
    def NodeFromReturningAgent = Channel.createOne2One()
    
    def NodeToInitialAgentInEnd = NodeToInitialAgent.in()
    def NodeToVisitingAgentInEnd = NodeToVisitingAgent.in()
    def NodeFromVisitingAgentOutEnd = NodeFromVisitingAgent.out()
    def NodeFromReturningAgentOutEnd = NodeFromReturningAgent.out()
    
    def myAgent = new AdaptiveAgent()
    myAgent.connect([NodeToInitialAgentInEnd, agentReturnChannelLocation, nodeId])
    def initialPM = new ProcessManager(myAgent)
    initialPM.start()
    
    def nodeAlt = new ALT([fromDataGen, agentVisitChannel, agentReturnChannel])
    def currentVisitList = [ ] 
                             
    while (true) {
      switch ( nodeAlt.select() ) {
        case 0:
          def d = fromDataGen.read()
          if ( d instanceof AvailableNodeList ) {
            d.anl.remove(agentVisitChannelLocation)  // list will contain this node's visit location
            currentVisitList = [ ]
            for ( i in 0 ..< d.anl.size) { currentVisitList << d.anl[i] }
            NodeToInitialAgent.out().write(currentVisitList)
            //println "NP-$nodeId: received visit list from DataGen with ${currentVisitList.size} elements"
          }
//        must be a data type name 
          else { 
            def dType = d.getClass().getName()
            //println "NP-$nodeId: data of type $dType being processed"
            if ( typeOrder.contains(dType) ) {
              def i = 0
              def notFound = true
              while (notFound) {
                if (typeOrder[i] == dType) {
                  notFound = false
                }
                else {
                  i = i + 1
                }
              }
              //println "NP-$nodeId: [${d.toString()}] sent to process $i"
              connectChannels[i].out().write(d)
              //println "NP-$nodeId: data of type $dType sent to Gatherer from $i'th process"
             }
            else {  
              // do not have process for this data type
              if ( ! currentSearches.contains(dType)) {
                currentSearches << dType
                //println "NP-$nodeId: data of type $dType search initiated"
                NodeToInitialAgent.out().write(dType)
                initialPM.join()
                // create a new initial agent
                myAgent = new AdaptiveAgent()
                myAgent.connect([NodeToInitialAgentInEnd, agentReturnChannelLocation, nodeId])
                initialPM = new ProcessManager(myAgent)
                initialPM.start()
                NodeToInitialAgent.out().write(currentVisitList)
                //println "NP-$nodeId: myAgent re created"
              }
              else {
                //println "NP-$nodeId: data of type $dType already being searched for"                
              }
            }            
          }
          break
        case 1:
          //println "NP-$nodeId: visiting agent has arrived"
          def visitingAgent = agentVisitChannel.read()
          visitingAgent.connect([NodeToVisitingAgentInEnd, 
                                 NodeFromVisitingAgentOutEnd ])
          def visitPM = new ProcessManager(visitingAgent)
          visitPM.start()
          def typeRequired = NodeFromVisitingAgent.in().read()
          //println "NP-$nodeId: visiting agent wants $typeRequired"
          if ( vanillaOrder.contains(typeRequired) ) {
            def i = 0
            def notFound = true
            while (notFound) {
              if (vanillaOrder[i] == typeRequired) {
                notFound = false
              }
              else {
                i = i + 1
              }            
            }
            NodeToVisitingAgent.out().write(vanillaList[i])
            def agentHome = NodeFromVisitingAgent.in().read()
            // the agent will have disconnected the internal connection
            //hence we must remake the connection net time we enter the loop
            println "NP-$nodeId: process for $typeRequired written to agent from $agentHome"
          }
          else {  // do not have process for this data type
            //println "NP-$nodeId: process for $typeRequired not available here"
            NodeToVisitingAgent.out().write(null)              
          }
          visitPM.join()
          break
        case 2:
          //println "NP-$nodeId: returned agent has arrived"
          def returnAgent = agentReturnChannel.read()
          returnAgent.connect([NodeFromReturningAgentOutEnd])
          def returnPM = new ProcessManager (returnAgent)
          returnPM.start()
          def returnList = NodeFromReturningAgent.in().read()
          returnPM.join()
          def returnedType = returnList[1]
          //println "NP-$nodeId: returned agent has brought process for $returnedType"
          currentSearches.remove([returnedType])
          typeOrder << returnList[1] 
          connectChannels[cp] = Channel.createOne2One()
          processList << returnList[0]               
          def pList = [connectChannels[cp].in(), nodeId, toGathererName]
          processList[cp].connect(pList)
          def pm = new ProcessManager(processList[cp])
          //println "NP-$nodeId: revised typeOrder is $typeOrder"
          println "NP-$nodeId: has started $returnedType Process"
          cp = cp + 1
          pm.start()          
          break
      }    
    }    
  }

}