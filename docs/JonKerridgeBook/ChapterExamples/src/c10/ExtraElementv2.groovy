package c10

import org.jcsp.lang.*
import org.jcsp.groovy.*

class ExtraElementv2 implements CSProcess { 
	
  def ChannelInput fromRing
  def ChannelOutput toRing
  
  def void run () {
    println "Extra Element v2 starting ..."
    while (true) {
      toRing.write( fromRing.read() )   
    }
  }
}
 
      
