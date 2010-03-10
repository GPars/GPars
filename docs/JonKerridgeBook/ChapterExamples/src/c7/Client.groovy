package c7

import org.jcsp.lang.*
import org.jcsp.groovy.*


class Client implements CSProcess{
  
  def ChannelInput receiveChannel
  def ChannelOutput requestChannel
  def clientNumber 
  
  def selectList = [ ]
  
  void run () {
    def iterations = selectList.size
    println "Client $clientNumber has $iterations values in $selectList"
    for ( i in 0 ..< iterations) {
      def key = selectList[i]
      requestChannel.write(key)
      def v = receiveChannel.read()
      println "Client $clientNumber: with $key has value $v"
    }
    println "Client $clientNumber has finished"
  }
}
