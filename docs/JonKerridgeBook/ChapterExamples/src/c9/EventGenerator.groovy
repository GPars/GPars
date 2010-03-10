package c9
  
import org.jcsp.lang.*
import org.jcsp.groovy.*

class EventGenerator implements CSProcess {
  
  def ChannelOutput outChannel
  def int source = 0
  def int initialValue = 0
  def int minTime = 100
  def int maxTime = 1000
  def int iterations = 10
  
  def void run () {
    
    One2OneChannel es2udd = Channel.createOne2One()
    
    println "Event Generator for source ${source} has started"
    
    def eventGeneratorList = [ 
            new EventStream ( source: source, 
                              initialValue: initialValue, 
                              iterations: iterations, 
                              outChannel: es2udd.out() ),
            new UniformlyDistributedDelay ( minTime: minTime, 
                                            maxTime: maxTime, 
                                            inChannel: es2udd.in(), 
                                            outChannel: outChannel )
            ]
    new PAR (eventGeneratorList).run()
  }
}
    