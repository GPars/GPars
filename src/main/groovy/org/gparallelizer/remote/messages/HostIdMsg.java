package org.gparallelizer.remote.messages;

import org.gparallelizer.remote.RemoteTransportProvider;

/**
 * Message sent by NetTransportProvider immediately after connection to another host set up set up
 *
 * @author Alex Tkachman
 */
public class HostIdMsg extends BaseMsg {

    /**
     * Construct message representing current state of the transport provider
     *
     * @param provider transport provider
     */
    public HostIdMsg(RemoteTransportProvider provider) {
        super(provider.getId());
    }
}
