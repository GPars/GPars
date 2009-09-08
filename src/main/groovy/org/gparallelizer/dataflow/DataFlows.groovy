//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.dataflow

import org.gparallelizer.dataflow.DataFlowVariable as DF
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Convenience class that makes working with DataFlowVariables more comfortable.
 *
 * See the implementation of {@link org.gparallelizer.samples.dataflow.DemoDataFlows} for a full example.
 *
 * A DataFlows instance is a bean with properties of type DataFlowVariable.
 * Property access is relayed to the access methods of DataFlowVariable.
 * Each property is initialized lazily the first time it is accessed.
 * Non-String named properties can be also accessed using array-like indexing syntax
 * This allows a rather compact usage of DataFlowVariables like
 *
 * <pre>
final df = new DataFlows()
start { df[0] = df.x + df.y }
start { df.x = 10 }
start { df.y = 5 }
assert 15 == df[0]
 * </pre>
 *
 * @author Vaclav Pech, Dierk Koenig, Alex Tkachman
 * Date: Sep 3, 2009
 */
public class DataFlows {

    private final static DF DUMMY = new DF()

    private ConcurrentMap variables = null

    // copy from ConcurrentHashMap for jdk 1.5 backwards compatibility
    static final int    DEFAULT_INITIAL_CAPACITY = 16
    static final float  DEFAULT_LOAD_FACTOR = 0.75f
    static final int    DEFAULT_CONCURRENCY_LEVEL = 16
    static final int    MAX_SEGMENTS = 1 << 16;

    /**
     * Constructor that supports the various constructors of the underlying
     * ConcurrentHashMap (unless the one with Map parameter).
     * @see java.util.concurrent.ConcurrentHashMap
     */
    DataFlows(
            int initialCapacity = DEFAULT_INITIAL_CAPACITY,
            float loadFactor    = DEFAULT_LOAD_FACTOR,
            int concurrencyLevel= DEFAULT_CONCURRENCY_LEVEL) {
        variables = new ConcurrentHashMap(initialCapacity, loadFactor, concurrencyLevel)
    }

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
     * Invokes the given method.
     * Allows for invoking whenBound() on the dataflow variables.
     * <pre>
     * def df = new DataFlows()
     * df.var {
     *     println "Variable bound to $it"
     * }
     * </pre>
     *
     * @param name the name of the method to call (the variable name)
     * @param args the arguments to use for the method call (a closure to invoke when a value is bound)
     * @return the result of invoking the method (void)
     */
    def invokeMethod(String name, Object args) {
        def df = ensureToContainVariable(name)
        if (args instanceof Object[] && args.length == 1 && args[0] instanceof Closure) {
            df >> args[0]
            return this
        } else
            throw new MissingMethodException(name, DataFlows, args)
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
          df = putNewUnderLock(name)
        }
        df
	}

  /**
   * Utility method extracted just to help JIT
   *
   * @return DFV
   */
    private def putNewUnderLock(name) {
      synchronized (this) {
        def df = variables.get(name)
        if (!df || (df == DUMMY)) {
          df = new DF()
          variables.put(name, df);
        }
        return df
      }
    }

    public def remove(name) {
        synchronized(this) {
            def df = variables.remove(name)
            df.bindSafely(null)
        }
    }
}
