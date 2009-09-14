package org.gparallelizer.remote;

import org.gparallelizer.remote.serial.RemoteSerialized;
import org.gparallelizer.MessageStream;

import java.util.UUID;

/**
 * @author Alex Tkachman
 */
public class RemoteMessageStream extends MessageStream implements RemoteSerialized {
    private RemoteHost remoteHost;

    @Override
    public void initDeserial(UUID serialId) {
        remoteHost = RemoteHost.getThreadContext();
        this.serialId = serialId;
    }

    public MessageStream send(Object message) {
        remoteHost.send(this, message);
        return this;
    }
}