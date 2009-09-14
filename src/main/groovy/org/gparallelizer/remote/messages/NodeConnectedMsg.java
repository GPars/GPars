package org.gparallelizer.remote.messages;

import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.actors.Actor;

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
    public final UUID nodeId;

    public final Actor mainActor;

    public NodeConnectedMsg(LocalNode node, UUID hostId) {
        super(hostId);
        nodeId = node.getId();
        mainActor = node.getMainActor();
    }
}
