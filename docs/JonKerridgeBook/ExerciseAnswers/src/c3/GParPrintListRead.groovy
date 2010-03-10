package c3

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.plugNplay.ProcessRead
 
class GParPrintListRead implements CSProcess {
	  def ChannelInputList inChannels
	  def List headings
	  def long delay = 200
	  
	  void run() {
	    def inSize = inChannels.size()

	    if ( headings == null ) {
	      println "No headings provided"
	    }
	    else {
	      headings.each { print "\t${it}" }
	      println ()
	    }

	    def timer = new CSTimer()
	    while ( true) {
		  def readerList = inChannels.read()
	      readerList.each { print "\t$it"  }
	      println ()
	      if (delay > 0 ) {
	        timer.sleep ( delay)
	      }
	    }
	  }
	}