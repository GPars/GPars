package org.gparallelizer.remote.messages;

import java.io.Serializable;
import java.util.UUID;

/**
 * Base class for all messages
 *
 * @author Alex Tkachman
 */
public class BaseMsg implements Serializable {
    public final UUID hostId;

    public BaseMsg(UUID hostId) {
        this.hostId = hostId;
    }
}
