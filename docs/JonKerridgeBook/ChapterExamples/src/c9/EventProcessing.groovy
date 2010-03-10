package c9;
   
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*
import org.jcsp.groovy.util.*

class EventProcessing implements CSProcess{
  
  def ChannelInputList eventStreams
  def minTime = 500
  def maxTime = 750

  void run() {
    One2OneChannel mux2udd = Channel.createOne2One()
    One2OneChannel udd2prn = Channel.createOne2One()
    
    def pList = [  
                   new FairMultiplex ( inChannels: eventStreams,
                                        outChannel: mux2udd.out() ),
/*                   
                   new PriMultiplex ( inChannels: eventStreams,
                                      outChannel: mux2udd.out() ),                     
*/                   

/*                   
                   new Multiplexer ( inChannels: eventStreams,
                                     outChannel: mux2udd.out() ),                     
*/

                   new UniformlyDistributedDelay ( inChannel:mux2udd.in(), 
                                                   outChannel: udd2prn.out(), 
                                                   minTime: minTime, 
                                                   maxTime: maxTime ), 
                   new GPrint ( inChannel: udd2prn.in(),
         		                 heading : "Event Output",
         		                 delay: 0)                    
                 ]
    new PAR (pList).run()
  }
}