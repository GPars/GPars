package groovyx.gpars.remote.message;

import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class CloseConnectionMsg extends SerialMsg {

    @Override
    public void execute(RemoteConnection conn) {
        conn.disconnect();
    }
}
