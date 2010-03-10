package c12.canteen;

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class Kitchen implements CSProcess{
  
  def ChannelOutput supply

  void run() {
    
    One2OneChannel console = Channel.createOne2One()
     
    def chef = new Chef ( supply: supply,
                          toConsole: console.out() )
    def chefConsole = new GConsole ( toConsole: console.in(),
                                             frameLabel: "Chef")
    new PAR([chef, chefConsole]).run()
  }

}