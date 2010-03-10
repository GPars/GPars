package c16
 
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import phw.util.*


class PrintUser implements CSProcess {
  
  def ChannelOutput printerRequest
  def ChannelOutput printerRelease
  def int userId
  
  void run() {
    def timer = new CSTimer()
    
    def printList = [ "line 1 for user " + userId, 
                      "line 2 for user " + userId,
                      "last line for user " + userId 
                    ]
    // create a channel upon which the PrintSpooler can send the printChannelLocation
    def useChannel = NetChannelEnd.createNet2One()
    // write the location to the PrintSpooler using the printRequestChannel
    printerRequest.write(new PrintJob ( userId: userId,
                                         useLocation: useChannel.getChannelLocation() ) )
    // read the printChannelLocation from the use channel
    def printChannelLocation = useChannel.read()
    def useKey = useChannel.read()
    println "Print for user ${userId} accepted using Spooler $useKey"
    //create the output printerChannel 
    def printerChannel = NetChannelEnd.createOne2Net ( printChannelLocation)    
    // now spool the output to PrintSpooler    
    printList.each { printerChannel.write (new Printline ( printKey: useKey, line: it) )
                     timer.sleep(10) }    
    // now release the connection to the PrintSpooler
    printerRelease.write ( useKey )
    println "Print for user ${userId} completed"
  }
}