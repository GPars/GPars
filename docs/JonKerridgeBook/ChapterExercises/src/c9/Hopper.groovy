package c9

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.ChannelOutput

class Hopper implements CSProcess {
	
	def ChannelInput fromConsole
	def ChannelOutput toConsole
	def ChannelOutput clearConsole
	def ChannelOutput toManager
	def ChannelInput fromManager	
	def int hopper
	
	void run () {
		while (true) {
			toConsole.write("Input an r when Hopper ready\n")
			fromConsole.read() 				// read the r from console
			toManager.write(1) 				// tell the manager this hopper is ready
			fromManager.read() 				// manager telling hopper to start emptying
			clearConsole.write("\n")  		// clear the console input area of the r
			toConsole.write("Emptying\n") 	// and write emptying to the console
			toManager.write(2) 				// this request tells the manager that we are ready to stop
			fromManager.read() 				// this is the manager telling us to stop emptying
			toConsole.write("Cycle complete - refill hopper\n")
		}
	}
}