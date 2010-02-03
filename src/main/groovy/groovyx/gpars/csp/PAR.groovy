package groovyx.gpars.csp

import org.jcsp.lang.Parallel

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
 * @version 1.1 modified to compile under groovy jsr03
 */

/**
  * PAR is used to create a <code>Parallel</code> object
  **/

class PAR extends Parallel {
 
/**
  * PAR extends the <code>Parallel</code> class of JCSP
  * PAR takes a list of processes as its constructor parameter
  * and converts them to an array of <code>CSProcess</code> 
  * as required by <code>Parallel</code>
  **/
  
  PAR(processList){
    super ()
    processList.each{p-> 
                     this.addProcess(p)
                    }
  }
  
  PAR(){
    super()
  }
  
}
