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
			for ( i in 0 ..< hoppers) inputs[i].read()
			// now read 1 from the blender
			inputs[hoppers].read()
			// now send a response to the hoppers and the blender
			for ( i in 0 .. hoppers ) outputs[i].write(1)
			// now read terminating 2 from blender
			inputs[hoppers].read()
			// and read the termninating 2 from the hoppers
			for ( i in 0 ..< hoppers) inputs[i].read()
			// now send a response to the hoppers and the blender
			for ( i in 0 .. hoppers ) outputs[i].write(1)
		}
	}	
}
