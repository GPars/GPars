package c13
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class Read implements CSProcess {
  
  def ChannelOutput r2db
  def ChannelInput db2r
  def int id
  def ChannelOutput toConsole
  
  void run () {
	def timer = new CSTimer()
    toConsole.write ( "Reader has started \n")
    for ( i in 0 ..<10 ) {
      def d = new DataObject(pid:id)
      d.location = i
      r2db.write(d)
      d = db2r.read()
      toConsole.write ( "Location " +  d.location + " has value " + d.value + "\n")
      timer.sleep(100)
    }
    toConsole.write ( "Reader has finished \n")
  }
}
