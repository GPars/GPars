package c18

import org.jcsp.lang.*
import org.jcsp.groovy.MobileAgent

class Agent implements MobileAgent {
  
  def ChannelOutput toLocal
  def ChannelInput fromLocal
  def results = [ ]
                  
  def connect ( List c ) {
    this.toLocal = c[0]
    this.fromLocal = c[1]
  }
  
  def disconnect () {
    toLocal = null
    fromLocal = null
  }
  
  void run() {
    toLocal.write (results)
    results = fromLocal.read()
  }

}