package c20
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class Queue implements CSProcess {

  def ChannelInput fromElement
  def ChannelInput fromPrompter
  def ChannelOutput toStateManager
  def ChannelOutput toPrompter
  def int slots

  void run() {
    def qAlt = new ALT ( [ fromElement, fromPrompter ] )
    def preCon = new boolean[2]
    def ELEMENT = 0
    def PROMPT = 1
    preCon[ELEMENT] = true    
    preCon[PROMPT] = false  
    def data = []
    def counter = 0       
    def front = 0       
    def rear = 0     
    while (true) {
      def index = qAlt.priSelect(preCon)
      switch (index) {
        case ELEMENT:
          data[front] = fromElement.read()
          front = (front + 1) % slots
          counter = counter + 1
          toStateManager.write(counter)
          break
        case PROMPT:
          fromPrompter.read()      
          toPrompter.write( data[rear])
          rear = (rear + 1) % slots
          counter = counter - 1
          toStateManager.write(counter)
          break
      }
      preCon[ELEMENT] = (counter < slots)
      preCon[PROMPT] = (counter > 0 )
    }
  }

}