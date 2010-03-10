package c17.sniff

import org.jcsp.lang.*
import org.jcsp.groovy.*
 
class SnifferComparator implements CSProcess {
  
  def ChannelInput fromCopy
  def ChannelInput fromScaler
  def interval = 10000

  void run() {
    def One2OneChannel connect = Channel.createOne2One()
    
    def testList = [ new Sniffer ( fromSystemCopy: fromCopy,
                                    toComparator: connect.out(),
                                    sampleInterval: interval), 
                     new Comparator ( fromSystemOutput: fromScaler,
                                       fromSniffer: connect.in() )
                    ]
    new PAR(testList).run()
  }

}