package c8

import org.jcsp.groovy.*
import org.jcsp.lang.*

class CSMux implements CSProcess {
  def ChannelInputList inClientChannels
  def ChannelOutputList outClientChannels
  def ChannelInputList fromServers
  def ChannelOutputList toServers
  def serverAllocation = [ ]	// list of lists of keys contained in each server
  void run() {
    def servers = toServers.size()
    def muxAlt = new ALT (inClientChannels)
    while (true) {
      def index = muxAlt.select()
      def key = inClientChannels[index].read()
      def server = -1
      for ( i in 0 ..< servers) {
        if (serverAllocation[i].contains(key)) {
          server = i
          break
        }        
      }
      toServers[server].write(key) 
      def value = fromServers[server].read()
      outClientChannels[index].write(value)        
    }
  }
}