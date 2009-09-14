package org.gparallelizer.dataflow;

import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Set;

/**
 * Data flow expression which invokes method of object
 *
 * @author Alex Tkachman
 */
public class DataFlowInvocationExpression extends DataFlowComplexExpression{
    private Object receiver;
    private final String methodName;

    public DataFlowInvocationExpression(Object receiver, String methodName, Object [] args) {
        super(args);
        this.receiver = receiver;
        this.methodName = methodName;
        init ();
    }

    protected Object evaluate() throws InterruptedException {
        if (receiver instanceof DataFlowExpression)
            receiver = ((DataFlowExpression)receiver).getVal();

        for (int i = 0; i != args.length; ++i)
            if (args[i] instanceof DataFlowExpression)
                args[i] = ((DataFlowExpression) args[i]).getVal();

        return InvokerHelper.invokeMethod(receiver, methodName, args);
    }

    protected void collectVariables(Set collection) {
        if (receiver instanceof DataFlowExpression)
            ((DataFlowExpression)receiver).collectVariables(collection);

        super.collectVariables(collection);
    }
}
