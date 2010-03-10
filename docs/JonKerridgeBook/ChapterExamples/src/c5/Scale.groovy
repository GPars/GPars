package c5
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class Scale implements CSProcess {
  def int scaling = 2
  def int multiplier = 2
  def ChannelOutput outChannel
  def ChannelOutput factor
  def ChannelInput inChannel
  def ChannelInput suspend
  def ChannelInput injector
  
  void run () {
    def SECOND = 1000
    def DOUBLE_INTERVAL = 5 * SECOND
    def NORMAL_SUSPEND  = 0
    def NORMAL_TIMER    = 1
    def NORMAL_IN       = 2
    def SUSPENDED_INJECT = 0
    def SUSPENDED_IN     = 1
    
    def timer = new CSTimer()
    def normalAlt = new ALT ( [ suspend, timer, inChannel ] )
    def suspendedAlt = new ALT ( [ injector, inChannel ] )
    
    def timeout = timer.read() + DOUBLE_INTERVAL
    timer.setAlarm ( timeout )
    while (true) {
      switch ( normalAlt.priSelect() ) {
        case NORMAL_SUSPEND :
          suspend.read()          // its a signal, no data content
          factor.write(scaling)   //reply with current value of scaling
          def suspended = true
          println "Suspended"
          while ( suspended ) {
            switch ( suspendedAlt.priSelect() ) {
              case SUSPENDED_INJECT:
                scaling = injector.read()   //this is the resume signal as well
                println "Injected scaling is ${scaling}"
                suspended = false
                timeout = timer.read() + DOUBLE_INTERVAL
                timer.setAlarm ( timeout )
                break
              case SUSPENDED_IN:
                def inValue = inChannel.read()
                def result = new ScaledData()
                result.original = inValue
                result.scaled = inValue
                outChannel.write ( result )  
                print "*"
                break
            }  // end-switch
          } //end-while
          break
        case NORMAL_TIMER:
          timeout = timer.read() + DOUBLE_INTERVAL
          timer.setAlarm ( timeout )
          scaling = scaling * multiplier
          println "Normal Timer: new scaling is ${scaling}"
          break
        case NORMAL_IN:
          def inValue = inChannel.read()
          def result = new ScaledData()
          result.original = inValue
          result.scaled = inValue * scaling
          outChannel.write ( result )  
          print"."
          break
      } //end-switch
    } //end-while
  } //end-run
}