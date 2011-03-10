package c3 

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class DifferentiateNeg implements CSProcess {
  
  def ChannelInput  inChannel
  def ChannelOutput outChannel
  
  void run() {
    
    One2OneChannel a = Channel.createOne2One()
    One2OneChannel b = Channel.createOne2One()
    One2OneChannel c = Channel.createOne2One()
    One2OneChannel d = Channel.createOne2One()
    
    def differentiateList = [ new GPrefix ( prefixValue: 0, 
                                            inChannel: b.in(), 
                                            outChannel: c.out() ),
                              new GPCopy ( inChannel: inChannel,  
                            		       outChannel0: a.out(), 
                            		       outChannel1: b.out() ),
                              //insert a constructor for Negator
                              new GPlus  ( inChannel0: a.in(), 
                            		       inChannel1: d.in(), 
                            		       outChannel: outChannel ) 
                            ]
    
    new PAR ( differentiateList ).run()
    
  }
}
