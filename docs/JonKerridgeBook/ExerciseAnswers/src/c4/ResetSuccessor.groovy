package c4


import org.jcsp.lang.*
import org.jcsp.groovy.*

class ResetSuccessor implements CSProcess {
	  
  def ChannelOutput outChannel
  def ChannelInput  inChannel
  def ChannelInput  resetChannel
	  
  void run () {
    def guards = [ resetChannel, inChannel  ]
    def alt = new ALT ( guards )
	while (true) {
	  def index = alt.priSelect()
	  if (index == 0 ) {    // resetChannel input
	    def resetValue = resetChannel.read()
	    def inputValue = inChannel.read() 
	    outChannel.write(resetValue)
	  }
	  else {    //inChannel input only
	    def inputValue = inChannel.read() 
	    outChannel.write(inputValue + 1)        
	  }
	}
  }
}
