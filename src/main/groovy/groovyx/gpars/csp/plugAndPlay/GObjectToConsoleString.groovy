package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.ChannelOutput

class GObjectToConsoleString implements CSProcess {

	def ChannelInput inChannel
	def ChannelOutput outChannel
		
		def void run () {
			while (true) {
				def o = inChannel.read()
				outChannel.write ( o.toString() + "\n" )
			}
		}

}