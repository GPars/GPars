package c8;

import org.jcsp.groovy.*
import org.jcsp.lang.*

class Server implements CSProcess{  
  def ChannelInputList fromMux
  def ChannelOutputList toMux  
  def dataMap = [ : ]                  
  void run() {
    def serverAlt = new ALT(fromMux)
    while (true) {
      def index = serverAlt.select()
      def key = fromMux[index].read()
      toMux[index].write(dataMap[key])
    }    
  }
}