package c9

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class Manager2Only implements CSProcess {
	
	def ChannelInputList inputs
	def ChannelOutputList outputs
	def ChannelInput fromBlender
	def ChannelOutput toBlender
	def int hoppers = 3

	void run(){
		def alt = new ALT(inputs)
		while (true) {
			def acceptedHoppers =[]
			// read from 2 of the hoppers and remember which are being used
			for ( ah in 0 .. 1) {
				acceptedHoppers[ah] = alt.select()
				inputs[acceptedHoppers[ah]].read()
				println "Accepted Hoppers is $acceptedHoppers"
			}
			// now read 1 from the blender
			fromBlender.read()
			// now send a response to the two accepted hoppers and the blender
			for ( i in 0 .. 1 ) outputs[acceptedHoppers[i]].write(1)
			toBlender.write(1)
			// now read terminating 2 from blender
			fromBlender.read()
			// and the termninating 2 from the hoppers
			for ( i in 0 .. 1) inputs[acceptedHoppers[i]].read()
			// now send a response to the hoppers and the blender
			for ( i in 0 .. 1 ) outputs[acceptedHoppers[i]].write(1)
			toBlender.write(1)
		}
	}	
}
