package groovyx.gpars.agent.remote;


import groovy.lang.Closure;
import groovyx.gpars.dataflow.DataflowVariable;

public class AgentClosureExecutionClosure extends Closure {
    private final DataflowVariable oldValueVariable;
    private final DataflowVariable newValueVariable;

    public AgentClosureExecutionClosure(Object owner, DataflowVariable oldValueVariable, DataflowVariable newValueVariable) {
        super(owner);
        this.oldValueVariable = oldValueVariable;
        this.newValueVariable = newValueVariable;
    }

    @Override
    public Object call(Object... args) {
        oldValueVariable.bindUnique(args[0]);
        try {
            invokeMethod("updateValue", newValueVariable.getVal());
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
}
