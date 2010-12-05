// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.dataflow;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.runtime.InvokerInvocationException;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Convenience class that makes working with DataFlowVariables more comfortable.
 * <p/>
 * See the implementation of groovyx.gpars.samples.dataflow.DemoDataFlows for a full example.
 * <p/>
 * A DataFlows instance is a bean with properties of type DataFlowVariable.
 * Property access is relayed to the access methods of DataFlowVariable.
 * Each property is initialized lazily the first time it is accessed.
 * Non-String named properties can be also accessed using array-like indexing syntax
 * This allows a rather compact usage of DataFlowVariables like
 * <p/>
 * <pre>
 * final df = new DataFlows()
 * start { df[0] = df.x + df.y }
 * start { df.x = 10 }
 * start { df.y = 5 }
 * assert 15 == df[0]
 * </pre>
 *
 * @author Vaclav Pech, Dierk Koenig, Alex Tkachman
 *         Date: Sep 3, 2009
 */
public final class DataFlows extends GroovyObjectSupport {

    private static final DataFlowVariable<Object> DUMMY = new DataFlowVariable<Object>();

    private final Object lock = new Object();

    @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})
    private ConcurrentMap<Object, DataFlowVariable<Object>> variables = null;

    // copy from ConcurrentHashMap for jdk 1.5 backwards compatibility
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * Constructor that supports the various constructors of the underlying
     * ConcurrentHashMap (unless the one with Map parameter).
     *
     * @param initialCapacity  the initial capacity. The implementation
     *                         performs internal sizing to accommodate this many elements.
     * @param loadFactor       the load factor threshold, used to control resizing.
     *                         Resizing may be performed when the average number of elements per
     *                         bin exceeds this threshold.
     * @param concurrencyLevel the estimated number of concurrently
     *                         updating threads. The implementation performs internal sizing
     *                         to try to accommodate this many threads.
     * @throws IllegalArgumentException if the initial capacity is
     *                                  negative or the load factor or concurrencyLevel are
     *                                  non-positive.
     * @see java.util.concurrent.ConcurrentHashMap
     */
    public DataFlows(final int initialCapacity, final float loadFactor, final int concurrencyLevel) {
        variables = new ConcurrentHashMap<Object, DataFlowVariable<Object>>(initialCapacity, loadFactor, concurrencyLevel);
    }

    /**
     * Constructor with default values for building the underlying ConcurrentHashMap
     *
     * @see java.util.concurrent.ConcurrentHashMap
     */
    public DataFlows() {
        variables = new ConcurrentHashMap<Object, DataFlowVariable<Object>>(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    /**
     * Binds the value to the DataFlowVariable that is associated with the property "name".
     *
     * @param newValue a scalar or a DataFlowVariable that may block on value access
     * @see DataFlowVariable#bind
     */
    @Override
    public void setProperty(final String property, final Object newValue) {
        ensureToContainVariable(property).bind(newValue);
    }

    /**
     * @return the value of the DataFlowVariable associated with the property "name".
     *         May block if the value is not scalar.
     * @see DataFlowVariable#getVal
     */
    @Override
    public Object getProperty(final String property) {
        try {
            return ensureToContainVariable(property).getVal();
        } catch (InterruptedException e) {
            throw new InvokerInvocationException(e);
        }
    }

    /**
     * Invokes the given method.
     * Allows for invoking whenBound() on the dataflow variables.
     * <pre>
     * def df = new DataFlows()
     * df.var {*     println "Variable bound to $it"
     * }* </pre>
     *
     * @param name the name of the method to call (the variable name)
     * @param args the arguments to use for the method call (a closure to invoke when a value is bound)
     * @return the result of invoking the method (void)
     */
    @Override
    public Object invokeMethod(final String name, final Object args) {
        final DataFlowVariable<Object> df = ensureToContainVariable(name);
        if (args instanceof Object[] && ((Object[]) args).length == 1 && ((Object[]) args)[0] instanceof Closure) {
            df.whenBound((Closure) ((Object[]) args)[0]);
            return this;
        } else
            throw new MissingMethodException(name, DataFlows.class, (Object[]) args);
    }

    /**
     * Retrieves the DFV associated with the given index
     *
     * @param index The index to find a match for
     * @return the value of the DataFlowVariable associated with the property "index".
     *         May block if the value is not scalar.
     * @throws InterruptedException If the thread gets interrupted
     * @see DataFlowVariable#getVal
     */
    @SuppressWarnings({"AutoBoxing"})
    Object getAt(final int index) throws InterruptedException {
        return ensureToContainVariable(index).getVal();
    }

    /**
     * Binds the value to the DataFlowVariable that is associated with the property "index".
     *
     * @param index The index to associate the value with
     * @param value a scalar or a DataFlowVariable that may block on value access
     * @see DataFlowVariable#bind
     */
    void putAt(final Object index, final Object value) {
        ensureToContainVariable(index).bind(value);
    }

    /**
     * The idea is following:
     * - we try to putIfAbsent dummy DFV in to map
     * - if something real already there we are done
     * - if not we obtain lock and put new DFV with double check
     * <p/>
     * Unfortunately we have to sync on this as there is no better option (God forbid to sync on name)
     *
     * @param name The key to ensure has a DFV bound to it
     * @return DataFlowVariable corresponding to name
     */
    private DataFlowVariable<Object> ensureToContainVariable(final Object name) {
        DataFlowVariable<Object> df = variables.putIfAbsent(name, DUMMY);
        if (df == null || df == DUMMY) {
            df = putNewUnderLock(name);
        }
        return df;
    }

    /**
     * Utility method extracted just to help JIT
     *
     * @param name The key to ensure has a DFV bound to it
     * @return a DFV associated with the key
     */
    private DataFlowVariable<Object> putNewUnderLock(final Object name) {
        synchronized (lock) {
            DataFlowVariable<Object> df = variables.get(name);
            //noinspection AccessToStaticFieldLockedOnInstance
            if (df == null || df == DUMMY) {
                df = new DataFlowVariable<Object>();
                variables.put(name, df);
            }
            return df;
        }
    }

    /**
     * Removes a DFV from the map and binds it to null, if it has not been bound yet
     *
     * @param name The name of the DFV to remove.
     * @return A DFV is exists, or null
     */
    public DataFlowVariable<Object> remove(final Object name) {
        synchronized (lock) {
            final DataFlowVariable<Object> df = variables.remove(name);
            if (df != null) df.bindSafely(null);
            return df;
        }
    }

    /**
     * Checks whether a certain key is contained in the map. Doesn't check, whether the variable has already been bound.
     *
     * @param name The name of the DFV to check.
     * @return A DFV is exists, or null
     */
    public boolean contains(final Object name) {
        return variables.containsKey(name);
    }

    /**
     * Convenience method to play nicely with Groovy object iteration methods.
     * The iteration restrictions of ConcurrentHashMap concerning parallel access and
     * ConcurrentModificationException apply.
     *
     * @return iterator over the stored key:DataFlowVariable value pairs
     */
    public Iterator<Map.Entry<Object, DataFlowVariable<Object>>> iterator() {
        return variables.entrySet().iterator();
    }
}
