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
 * @author Vaclav Pech, Dierk Koenig
 * Date: Sep 3, 2009
 */
public class DataFlows {

    private ConcurrentMap variables = new ConcurrentHashMap()

    /**
     * Binds the value to the DataFlowVariable that is associated with the property "name".
     * @param value a scalar or a DataFlowVariable that may block on value access
     * @see DataFlowVariable#bind
     */
    void setProperty(String name, value) {
        ensureToContainVariable(name)
        variables[name] << value
    }

    /**
     * @return the value of the DataFlowVariable associated with the property "name".
     * May block if the value is not scalar.
     * @see DataFlowVariable#getVal
     */
    def getProperty(String name) {
        ensureToContainVariable(name)
        variables[name].val
    }

    private ensureToContainVariable(String name) { 
	    variables.putIfAbsent(name, new DF()) 
	}
}