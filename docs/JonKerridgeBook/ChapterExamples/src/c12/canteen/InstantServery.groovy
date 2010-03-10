package c12.canteen;

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class InstantServery implements CSProcess{

  def ChannelInput service    
  def ChannelOutput deliver    
  def ChannelInput supply   
    
  void run() {
      
    One2OneChannel console = Channel.createOne2One()
      
    def servery = new InstantCanteen ( service: service,
                                        deliver: deliver,
                                        supply: supply,
                                        toConsole: console.out() )
    def serveryConsole = new GConsole ( toConsole: console.in(),
                                                frameLabel: "Instant Servery")
    new PAR([servery,serveryConsole]).run()
  }

}