package groovyx.gpars.dataflow.remote;

import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.expression.DataflowExpression;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.serial.RemoteSerialized;

public final class RemoteDataflowVariable<T> extends DataflowVariable<T> implements RemoteSerialized {
    private static final long serialVersionUID = -420013188758006693L;
    private final RemoteHost remoteHost;
    private boolean disconnected;

    public RemoteDataflowVariable(final RemoteHost host) {
        remoteHost = host;
        getValAsync(new MessageStream() {
            private static final long serialVersionUID = 7968302123667353660L;

            @SuppressWarnings({"unchecked"})
            @Override
            public MessageStream send(final Object message) {
                if (!disconnected) {
                    remoteHost.write(new DataflowExpression.BindDataflow(RemoteDataflowVariable.this, message, remoteHost.getHostId()));
                }
                return this;
            }
        });
    }
}
