package groovyx.gpars.csp

import org.jcsp.lang.CSProcess

interface MobileAgent extends CSProcess, Serializable {

   abstract connect(List x)
   abstract disconnect()
}