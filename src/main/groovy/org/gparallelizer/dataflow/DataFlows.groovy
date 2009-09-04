package org.gparallelizer.dataflow

import org.gparallelizer.dataflow.DataFlowVariable as DF
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Convenience class that makes working with DataFlowVariables more comfortable.
 * See the implementation of {@link org.gparallelizer.samples.dataflow.DemoDataFlows} for a full example.
 * A DataFlows instance is a bean with properties of type DataFlowVariable.
 * Property access is relayed to the access methods of DataFlowVariable.
 * Each property is initialized lazily the first time it is accessed.
 * This allows a rather compact usage of DataFlowVariables like
 * <pre>
final df = new DataFlows()
start { df.result = df.x + df.y }
start { df.x = 10 }
start { df.y = 5 }
assert 15 == df.result
 * </pre>
 *
 * @author Vaclav Pech, Dierk Koenig, Alex Tkachman
 * Date: Sep 3, 2009
 */
public class DataFlows {

  private final static DF DUMMY = new DF ()

    private ConcurrentMap variables = new ConcurrentHashMap()

    /**
     * Binds the value to the DataFlowVariable that is associated with the property "name".
     * @param value a scalar or a DataFlowVariable that may block on value access
     * @see DataFlowVariable#bind
     */
    void setProperty(String name, value) {
        ensureToContainVariable(name) << value
    }

    /**
     * @return the value of the DataFlowVariable associated with the property "name".
     * May block if the value is not scalar.
     * @see DataFlowVariable#getVal
     */
    def getProperty(String name) {
        ensureToContainVariable(name).val
    }

  /**
   * @return the value of the DataFlowVariable associated with the property "name".
   * May block if the value is not scalar.
   * @see DataFlowVariable#getVal
   */
    def getAt (index) {
      ensureToContainVariable(index).val
    }

  /**
   * Binds the value to the DataFlowVariable that is associated with the property "index".
   * @param value a scalar or a DataFlowVariable that may block on value access
   * @see DataFlowVariable#bind
   */
    void putAt (index,value) {
      ensureToContainVariable(index) << value
    }

   /**
    * The idea is following:
    * - we try to putIfAbsent dummy DFV in to map
    * - if something real already there we are done
    * - if not we obtain lock and put new DFV with double check
    *
    * Unfortunately we have to sync on this as there is no better option (God forbid to sync on name)
    *
    * @return DataFlowVariable corresponding to name
    */
    private def ensureToContainVariable(name) {
	    def df = variables.putIfAbsent(name, DUMMY)
        if (!df || df == DUMMY) {
          df = putNewUnderLock(name, df)
        }
        df
	}

  /**
   * Utility method extracted just to help JIT
   *
   * @return DFV
   */
    private def putNewUnderLock(name, df) {
      synchronized (this) {
        df = variables.get(name)
        if (df == DUMMY) {
          df = new DF()
          variables.put(name, df);
        }
      }
      return df
    }
}