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
			// read from 2 of the hoppers and remember which are being used
			// now read 1 from the blender
			// now send a response to the two accepted hoppers and the blender
			// now read terminating 2 from blender
			// and the termninating 2 from the hoppers
			// now send a response to the hoppers and the blender
		}
	}	
}
