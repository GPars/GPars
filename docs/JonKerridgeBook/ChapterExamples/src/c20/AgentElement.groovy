package c20
   
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.* 

class AgentElement implements CSProcess {
  
  def ChannelInput fromRing
  def ChannelOutput toRing
  def int element
  def int nodes
  def int iterations
  
  def void run() {
    One2OneChannel S2RE = Channel.createOne2One()
    One2OneChannel RE2Q = Channel.createOne2One()
    One2OneChannel SM2RE = Channel.createOne2One()
    One2OneChannel Q2P = Channel.createOne2One()
    One2OneChannel Q2SM = Channel.createOne2One()
    One2OneChannel P2Q = Channel.createOne2One()
    One2OneChannel P2R = Channel.createOne2One()
    One2OneChannel R2GEC = Channel.createOne2One()
    One2OneChannel R2GECClear = Channel.createOne2One()
    One2OneChannel GEC2R = Channel.createOne2One()
    
    def nodeList = [ new Sender ( toElement: S2RE.out(), 
                                   element: element, 
                                   nodes: nodes, 
                                   iterations: iterations),
                     new Receiver ( fromElement: P2R.in(), 
                                    fromConsole: GEC2R.in(),
                                    clear: R2GECClear.out(),
                                    outChannel: R2GEC.out() ),
                     new RingAgentElement ( fromSender: S2RE.in(),
                                            fromStateManager: SM2RE.in(),
                                            toQueue: RE2Q.out(),
                                            fromRing: fromRing, 
                                            toRing: toRing, 
                                            element: element ), 
                     new Queue ( fromElement: RE2Q.in(), 
                                 toStateManager: Q2SM.out(),
                                 fromPrompter:P2Q.in() ,
                                 toPrompter: Q2P.out(),
                                 slots: (nodes * 2) ),                           
                     new Prompter ( toQueue: P2Q.out(),
                                     fromQueue: Q2P.in(),
                                     toReceiver: P2R.out()), 
                     new StateManager ( fromQueue: Q2SM.in(),
                                         toElement: SM2RE.out(),
                                         queueSlots: (nodes * 2)  ),
                     new GConsole ( toConsole: R2GEC.in(),
                                    fromConsole: GEC2R.out(),
                                    clearInputArea: R2GECClear.in(),
                                    frameLabel: "Element: " + element)
                   ]
    new PAR ( nodeList ).run()
  }
}
    
