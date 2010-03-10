package c2

import org.jcsp.lang.*

class CreateSetsOfEight implements CSProcess{
	
	def ChannelInput inChannel

	void run(){
		def outList = []
		def v = inChannel.read()
		while (v != -1){
			for ( i in 0 .. 7 ) {
				// put v into outList and read next input
			}
			println " Eight Object is ${outList}"
		}
		println "Finished"
	}
}