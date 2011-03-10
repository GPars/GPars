package c3 

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*
 
class GPairs implements CSProcess {
  
  def ChannelOutput outChannel
  def ChannelInput  inChannel
  
  void run() {
    
    One2OneChannel a = Channel.createOne2One()
    One2OneChannel b = Channel.createOne2One()
    One2OneChannel c = Channel.createOne2One()
    
    def pairsList  = [ new GPlus   ( outChannel: outChannel, 
                                     inChannel0: a.in(),
                                     inChannel1: c.in() ),
                       new GSCopy  ( inChannel: inChannel, 
                                     outChannel0: a.out(), 
                                     outChannel1: b.out() ),
                       new GTail   ( inChannel: b.in(), 
                                     outChannel: c.out() ) 
                       ]
    new PAR ( pairsList ).run()
  }
}
