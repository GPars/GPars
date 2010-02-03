package groovyx.gpars.csp

import org.jcsp.lang.Channel
import org.jcsp.plugNplay.ProcessRead

/**
 * <p>Title: Groovy Classes to Implement JCSP parallelism</p>
 *
 * <p>Description: Classes required to implement JCSP Constructs within
 * Groovy</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: Napier University</p>
 *
 * @author Jon Kerridge, Ken Barclay, John Savage
 * @version 1.0
 *
 * @version 1.1 included the empty  constructor to enable 
 * easier <code>NetChannelInput</code> list creation (Jon Kerridge)
 * and changes to comply with Groovy-jsr03
 */

/**
  * ChannelInputList is used to create a list of 
  * <code>ChannelInputEnds</code> 
  **/

class ChannelInputList {
  def cList = []

/**
  * ChannelInputList uses the <code>ArrayList</code> class of java
  * This ChannelInputList takes an array of <code>One2OneChannels</code> as its 
  * constructor parameter and converts them to a list of <code>ChannelInputEnds</code>
  **/
  
  ChannelInputList( channelArray) {
    cList = ( Arrays.asList(Channel.getInputArray( channelArray )) )
  }
  
/**
  * ChannelInputList uses the <code>ArrayList</code> class of java
  * This constructor creates an empty <code>ArrayList</code> to be populated with
  * <code>NetChannelInputs</code>
  **/

  ChannelInputList() {
    // nothing required it is just an empty list
  }
  
  def append ( value) {
    cList << value
  }
  
  def size() {
    return cList.size()
  }
  
  def contains ( value ) {
    return cList.contains (value)
  }
  
  def remove ( value ) {
    return cList.remove ( value )
  }
  
  def minus ( list ) {
	  return cList - list
  }
  
  def plus ( list ) {
	  return cList + list
  }

  def putAt ( index, value ) {
    cList.set ( index, value )
  }
  
  def getAt ( index  ) {
    return cList.get (index ) 
  }
    
  def Object[] toArray () {
    return cList.toArray ()
  }

  def List read () {
	  def values = [ ]
	  def channels = cList.size()
	  def readerList = []
	  (0 ..< channels).each { i -> readerList [i] = new ProcessRead ( cList[i] )
	                        }
	  def parRead = new PAR ( readerList )
	  parRead.run()
	  (0 ..< channels).each { i -> values[i] = readerList[i].value }
	  return values
  }

}
