package org.gparallelizer.remote.messages;

import org.gparallelizer.remote.RemoteActor;
import org.gparallelizer.actors.Actor;

import java.util.UUID;

/**
 * Message sent to stop remote Actor
 *
 * @author Alex Tkachman
 */
public class StopActorMsg extends BaseMsg {
    public final Actor actor;

    public StopActorMsg(RemoteActor remoteActor, UUID hostId) {
        super(hostId);
        actor = remoteActor;
    }
}
