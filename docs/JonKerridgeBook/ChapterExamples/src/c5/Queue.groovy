package c5

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Queue implements CSProcess {
  
  def ChannelInput  put
  def ChannelInput  get
  def ChannelOutput receive
  def int elements = 5    
   
  void run() {
    def qAlt = new ALT ( [ put, get ] )
    def preCon = new boolean[2]
    def PUT = 0
    def GET = 1
    preCon[PUT] = true    
    preCon[GET] = false  
    def data = []
    def count = 0       
    def front = 0       
    def rear = 0     
    def running = true
    while (running) {
      def index = qAlt.priSelect(preCon)
      switch (index) {
        case PUT:
          data[front] = put.read()
          //println "Q: put ${data[front]} at ${front}"
          front = (front + 1) % elements
          count = count + 1
          break
        case GET:
          get.read()      
          receive.write( data[rear])
          if (data[rear] == null) {
        	  running = false
          }
          rear = (rear + 1) % elements
          count = count - 1
          break
      }
      preCon[PUT] = (count < elements)
      preCon[GET] = (count > 0 )
    }
    println "Q finished"
  }
}

          