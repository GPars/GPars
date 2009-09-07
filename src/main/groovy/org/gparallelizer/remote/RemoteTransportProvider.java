package org.gparallelizer.remote;

public abstract class RemoteTransportProvider {
    public abstract void connect(LocalNode node);

    public abstract void disconnect(LocalNode node);
}
