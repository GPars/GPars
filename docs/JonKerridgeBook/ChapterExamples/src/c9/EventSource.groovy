package c9
  
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*


class EventSource implements CSProcess {
  def source 
  def iterations = 99
  def minTime = 100
  def maxTime = 250
  def ChannelOutput outChannel
  
  void run() {
    One2OneChannel eg2h = Channel.createOne2One()
    
    def sourceList = [ new EventGenerator ( source: source,
                                            initialValue: 100 * source,
                                            iterations: iterations,
                                            minTime: minTime,
                                            maxTime: maxTime,
                                            outChannel: eg2h.out()),
                       new EventHandler ( inChannel: eg2h.in(),
                                          outChannel: outChannel)
                      ]

    new PAR (sourceList).run()
  }
}