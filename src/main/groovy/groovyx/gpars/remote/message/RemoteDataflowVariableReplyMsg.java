package groovyx.gpars.remote.message;

import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflows;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteDataflowVariableReplyMsg extends SerialMsg {

    private final String name;
    private final DataflowVariable variable;

    public RemoteDataflowVariableReplyMsg(String name, DataflowVariable variable) {
        this.name = name;
        this.variable = variable;
    }

    @Override
    public void execute(RemoteConnection conn) {
        DataflowVariable remoteVariable = conn.getLocalHost().getRemoteDataflowsRegistry().get(name);
        remoteVariable.bindUnique(variable);
    }

}
