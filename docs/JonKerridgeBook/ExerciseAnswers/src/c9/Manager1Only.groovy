package c9

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class Manager1Only implements CSProcess {
	
	def ChannelInputList inputs
	def ChannelOutputList outputs
	def ChannelInput fromBlender
	def ChannelOutput toBlender
	def int hoppers = 3

	void run(){
		def alt = new ALT(inputs)
		while (true) {
			// read 1 from one of the three hoppers
			def ah = alt.select()
			inputs[ah].read()
			println "Accepted Hopper is $ah"
			// now read 1 from the blender
			fromBlender.read()
			// now send a response to the accepted hopper and the blender
			outputs[ah].write(1)
			toBlender.write(1)
			// now read terminating 2 from blender
			fromBlender.read()
			// and the terminating 2 from the accepted hopper
			inputs[ah].read()
			// now send a response to the hopper and the blender
			outputs[ah].write(1)
			toBlender.write(1)
		}
	}	
}
