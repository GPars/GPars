package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.LazyDataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflows;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteDataflowVariableReplyMsg extends SerialMsg {

    private final String name;
    private final DataflowVariable variable;
    private final boolean bound;
    private Object value;

    public RemoteDataflowVariableReplyMsg(String name, DataflowVariable variable) {
        this.name = name;
        this.variable = variable;
        this.bound = variable.isBound();
        if (bound) {
            try {
                value = variable.getVal();
            } catch (InterruptedException e) {
                // fail silently
            }
        }
    }

    @Override
    public void execute(RemoteConnection conn) {
        conn.getLocalHost().registerProxy(RemoteDataflowVariable.class, name, ((RemoteDataflowVariable) variable));
        if (bound) {
            variable.bind(value);
        }
    }

}
