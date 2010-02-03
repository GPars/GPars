package groovyx.gpars.csp.plugAndPlay

import org.jcsp.lang.CSProcess
import org.jcsp.lang.ChannelInput
import org.jcsp.lang.ChannelOutput
import org.jcsp.awt.ActiveClosingFrame
import java.awt.BorderLayout
import java.awt.Label
import java.awt.Font
import org.jcsp.awt.ActiveTextArea
import org.jcsp.awt.ActiveTextEnterField
import java.awt.Container
import java.awt.GridLayout
import groovyx.gpars.csp.PAR

class GConsole implements CSProcess {

	def ChannelInput toConsole    
	def ChannelOutput fromConsole
	def ChannelInput clearInputArea
	def String frameLabel = "Groovy Eclipse Console"
	
	def void run () {
		def main = new ActiveClosingFrame (frameLabel)
		def root = main.getActiveFrame()
		root.setLayout ( new BorderLayout () )
		def outLabel = new Label ("Output Area", Label.CENTER)
		outLabel.setFont(new Font("sans-serif", Font.BOLD, 20))
		def inLabel = new Label ("Input Area", Label.CENTER)
		inLabel.setFont(new Font("sans-serif", Font.BOLD, 20))
		def outText = new ActiveTextArea ( toConsole, null )
		def inText = new ActiveTextEnterField ( clearInputArea, fromConsole )
		def console = new Container()
	    console.setLayout ( new GridLayout ( 4, 1 ) )
	    console.add ( outLabel )
	    console.add ( outText )
	    console.add ( inLabel )
	    console.add ( inText.getActiveTextField() )
	    root.add(console, BorderLayout.CENTER )
	    root.pack()
        root.setVisible(true)    
        def interfaceProcessList = [ main, outText, inText ]
        new PAR ( interfaceProcessList ).run()		
	}

}