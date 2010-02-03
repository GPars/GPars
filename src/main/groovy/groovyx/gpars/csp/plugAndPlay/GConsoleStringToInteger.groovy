package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.ChannelOutput

class GConsoleStringToInteger implements CSProcess {

	def ChannelInput inChannel
	def ChannelOutput outChannel
	
	def void run () {
		while (true) {
			def String s = inChannel.read().trim()
			def i = Integer.valueOf(s)
			outChannel.write ( i )
		}
	}
	
}