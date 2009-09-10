package org.gparallelizer.remote.messages;

import org.gparallelizer.remote.LocalNode;

import java.util.UUID;

/**
 * Message sent when local node connected to remote host
 *
 * @author Alex Tkachman
 */
public class NodeConnectedMsg extends BaseMsg {

    /**
     * Id of node connected
     */
    public  final UUID nodeId;

    public NodeConnectedMsg(LocalNode node, UUID hostId) {
        super(hostId);
        nodeId = node.getId();
    }
}
