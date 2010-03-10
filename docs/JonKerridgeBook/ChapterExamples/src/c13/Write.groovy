package c13
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class Write implements CSProcess {
  
  def ChannelOutput w2db
  def ChannelInput db2w
  def int id
  def ChannelOutput toConsole
  
  void run () {
	def timer = new CSTimer()
    toConsole.write ( "Writer has started \n" )
    for ( j in 0 ..<10 ) {
      def d = new DataObject(pid:id)
      def i = 9 - j    // write in reverse order
      d.location = i
      d.value = i + ((id+1)*1000)
      w2db.write(d)
      d = db2w.read()
      toConsole.write ( "Location " + d.location + " now contains " + d.value + "\n")
      timer.sleep(100)
    }
    toConsole.write ( "Writer has finished \n" )
  }
}    