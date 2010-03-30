package c9

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class ManagerAll3 implements CSProcess {
	
	def ChannelInputList inputs
	def ChannelOutputList outputs
	def int hoppers = 3

	void run(){
		while (true) {
			// read 1 from the three hoppers
			// now read 1 from the blender
			// now send a response to the hoppers and the blender
			// now read terminating 2 from blender
			// and read the termninating 2 from the hoppers
			// now send a response to the hoppers and the blender
		}
	}	
}
