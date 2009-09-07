package org.gparallelizer.remote;

public abstract class RemoteTransportProvider<T> {
    public abstract void connect(LocalNode node);

    public abstract void disconnect(LocalNode node);
}
