package c9;
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class EventHandler implements CSProcess {
  
  def ChannelInput inChannel
  def ChannelOutput outChannel
  
  def void run () {
    
    One2OneChannel get = Channel.createOne2One()
    One2OneChannel transfer = Channel.createOne2One()
    One2OneChannel toBuffer = Channel.createOne2One()
    
    def handlerList = [ new EventReceiver ( eventIn: inChannel, 
                                             eventOut: toBuffer.out()),
                        new EventOWBuffer ( inChannel: toBuffer.in(), 
                            getChannel: get.in(), outChannel: transfer.out() ),
                        new EventPrompter ( inChannel: transfer.in(), 
                            getChannel: get.out(), outChannel: outChannel )
                      ]
    new PAR ( handlerList ).run()
  }
}