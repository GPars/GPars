package groovyx.gpars.csp

import org.jcsp.lang.Alternative
import org.jcsp.lang.Guard

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
 * @version 1.1    takes account of jsr03 requirements
 */

/**
  * ALT is used to create an <code>Alternative</code> object
  **/

class ALT extends Alternative {
 
/**
  * ALT extends the <code>Alternative</code> class of JCSP
  * ALT takes a list of <code>Guards</code> as its constructor parameter
  * and converts them to an array of <code>Guards</code> 
  * as required by <code>Alternative</code>
  **/
  
  ALT (ChannelInputList channelList) {
    super( (Guard[]) channelList.toArray() )
    
  }
  ALT (List guardList) {
    super( (Guard[]) guardList.toArray() )
  }
  
}
