package c7

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Server implements CSProcess{
  
  def ChannelInput clientRequest
  def ChannelOutput clientSend
  
  def ChannelOutput thisServerRequest
  def ChannelInput thisServerReceive
  
  def ChannelInput otherServerRequest
  def ChannelOutput otherServerSend
  
  def dataMap = [ : ]
                  
  void run () {
    def CLIENT = 0
    def OTHER_REQUEST = 1
    def THIS_RECEIVE = 2
    def serverAlt = new ALT ([clientRequest, otherServerRequest, thisServerReceive])
    while (true) {
      def index = serverAlt.select()
      switch (index) {
        case CLIENT :
          def key = clientRequest.read()
          if ( dataMap.containsKey(key) ) {
            clientSend.write(dataMap[key])
          }
          else {
            thisServerRequest.write(key)
          } //end if 
          break
        case OTHER_REQUEST :
          def key = otherServerRequest.read()
          if ( dataMap.containsKey(key) ) {
            otherServerSend.write(dataMap[key])
          }
          else {
            otherServerSend.write(-1)
          } //end if 
          break
        case THIS_RECEIVE :
          clientSend.write(thisServerReceive.read() )
          break
      } // end switch              
    } //end while   
  } //end run
}