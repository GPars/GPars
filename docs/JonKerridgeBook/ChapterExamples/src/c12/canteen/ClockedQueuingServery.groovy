package c12.canteen

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class ClockedQueuingServery implements CSProcess{

  def ChannelInput service    
  def ChannelOutput deliver    
  def ChannelInput supply   
   
  void run() {
    
    Any2OneChannel console = Channel.createAny2One()
    
    def clock = new Clock ( toConsole: console.out() )
    
    def servery = new QueuingCanteen ( service: service,
                                        deliver: deliver,
                                        supply: supply,
                                        toConsole: console.out() )
    def serveryConsole = new GConsole ( toConsole: console.in(),
                                        frameLabel: "Clocked Queuing Servery")
    new PAR([servery, serveryConsole, clock ]).run()
  }

}