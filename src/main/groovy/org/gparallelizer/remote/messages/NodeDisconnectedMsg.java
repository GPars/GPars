package org.gparallelizer.remote.messages;

import org.gparallelizer.remote.LocalNode;

import java.util.UUID;

/**
 * Message sent when local node disconnected from remote host
 *
 * @author Alex Tkachman
 */
public class NodeDisconnectedMsg extends BaseMsg {

    /**
     * Id of node disconnected
     */
    public  final UUID nodeId;

    public NodeDisconnectedMsg(LocalNode node, UUID hostId) {
        super(hostId);
        nodeId = node.getId();
    }
}