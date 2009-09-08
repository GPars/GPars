package org.gparallelizer.remote.net;

import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.remote.RemoteTransportProvider;

public class NetTransportProvider extends RemoteTransportProvider<NetNode> {
    public NetTransportProvider() {
    }

    protected NetNode createRemoteNode(LocalNode node) {
        return null;
    }
}
