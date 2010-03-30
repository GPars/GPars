package c9

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.ChannelOutput

class Blender implements CSProcess {

	def ChannelInput fromConsole
	def ChannelOutput toConsole
	def ChannelOutput clearConsole
	def ChannelOutput toManager
	def ChannelInput fromManager	
	
	void run(){
		while (true) {
			toConsole.write("Input an r when Blender ready\n")
			fromConsole.read()  //this reads the enabling r from the console
			toManager.write(1)  // informs the manager the blender is ready
			fromManager.read()  // manager indicates that blending can start
			clearConsole.write("\n")  // clrea the console input area
			toConsole.write("Blending\n") // and write an appropriate message
			toConsole.write("Input an f when Blender finished\n")
			fromConsole.read() // input the f indicating that blending has finished
			toManager.write(2) // and tell the manager that blending has finished
			clearConsole.write("\n")
			fromManager.read() // manager confirms to the blender finish and all hoppers have stopped emptying
			toConsole.write("Cycle complete\n")
		}
		
	}	
}
