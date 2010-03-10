package c10

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.* 

class Elementv2 implements CSProcess {
  
  def ChannelInput fromRing
  def ChannelOutput toRing
  def int element
  def int nodes
  def int iterations = 12
  
  def void run() {
    One2OneChannel S2RE = Channel.createOne2One()
    One2OneChannel RE2R = Channel.createOne2One()
    One2OneChannel R2GEC = Channel.createOne2One()
    
    def nodeList = [ new Sender ( toElement: S2RE.out(), 
                                   element: element, 
                                   nodes: nodes, 
                                   iterations: iterations),
                     new Receiver ( fromElement: RE2R.in(), 
                                    outChannel: R2GEC.out(),
                                    element: element),
                     new RingElementv2 ( fromLocal: S2RE.in(), 
                                         toLocal: RE2R.out(), 
                                         fromRing: fromRing, 
                                         toRing: toRing, 
                                         element: element),
                     new GConsole ( toConsole: R2GEC.in(),
                                           frameLabel: "Element: " + element)
                   ]
    new PAR ( nodeList ).run()
  }
}
    
  