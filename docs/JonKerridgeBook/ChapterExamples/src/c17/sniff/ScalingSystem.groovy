package c17.sniff

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*
import c5.*

class ScalingSystem implements CSProcess{
  
  def ChannelOutput toSniffer
  def ChannelOutput toComparator

  void run() {
    
    def data1 = Channel.createOne2One()
    def data2 = Channel.createOne2One()
    def timedData = Channel.createOne2One()
    def scaledData = Channel.createOne2One()
    def oldScale = Channel.createOne2One()
    def newScale = Channel.createOne2One()
    def pause = Channel.createOne2One()

    def network = [ new GNumbers ( outChannel: data1.out() ),
                    new GPCopy ( inChannel: data1.in(),
                                  outChannel0: toSniffer,
                                  outChannel1: data2.out()),
                    new GFixedDelay ( delay: 500, 
                        		      inChannel: data2.in(), 
                        		      outChannel: timedData.out() ),
                    new Scale ( inChannel: timedData.in(),
                                outChannel: toComparator,
                                factor: oldScale.out(),
                                suspend: pause.in(),
                                injector: newScale.in(),
                                multiplier: 4,
                                scaling: 2 ),
                    new Controller ( testInterval: 7000,
                    		         computeInterval: 700,
                    		         addition: 3,
                                     factor: oldScale.in(),
                                     suspend: pause.out(),
                                     injector: newScale.out() )
                 ]
    new PAR ( network ).run()                                                            

    
  }

}